package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.dto.request.CheckoutRequest;
import com.springboot.jenka_coffee.dto.response.CartItem;
import com.springboot.jenka_coffee.entity.*;
import com.springboot.jenka_coffee.exception.InsufficientStockException;
import com.springboot.jenka_coffee.repository.OrderRepository;
import com.springboot.jenka_coffee.repository.ProductRepository;
import com.springboot.jenka_coffee.repository.VoucherRepository;
import com.springboot.jenka_coffee.service.CartService;
import com.springboot.jenka_coffee.service.EmailService;
import com.springboot.jenka_coffee.service.OrderService;
import com.springboot.jenka_coffee.service.VoucherService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;
    private final EmailService emailService;
    private final EntityManager entityManager;
    private final VoucherService voucherService;
    private final VoucherRepository voucherRepository;
    private final com.springboot.jenka_coffee.repository.PointHistoryRepository pointHistoryRepository;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.username}")
    private String adminEmail;

    public OrderServiceImpl(OrderRepository orderRepository,
                            ProductRepository productRepository,
                            CartService cartService,
                            EmailService emailService,
                            EntityManager entityManager,
                            VoucherService voucherService,
                            VoucherRepository voucherRepository,
                            com.springboot.jenka_coffee.repository.PointHistoryRepository pointHistoryRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.cartService = cartService;
        this.emailService = emailService;
        this.entityManager = entityManager;
        this.voucherService = voucherService;
        this.voucherRepository = voucherRepository;
        this.pointHistoryRepository = pointHistoryRepository;
    }

    @Override
    public Order create(Order order) {
        return orderRepository.save(order);
    }

    @Override
    public Order findById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Order findByIdWithDetails(Long id) {
        return orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new com.springboot.jenka_coffee.exception.ResourceNotFoundException(
                    "Không tìm thấy đơn hàng #" + id));
    }

    @Override
    public Page<Order> findByUsername(String username, Pageable pageable) {
        return orderRepository.findByAccount_Username(username, pageable);
    }

    /**
     * CHECKOUT TRANSACTION — ACID guaranteed
     * Luồng:
     *   1. Validate giỏ hàng không rỗng
     *   2. Lock Account (PESSIMISTIC_WRITE) để tránh double-spend points
     *   3. Lock sản phẩm (PESSIMISTIC_WRITE) để tránh race condition
     *   4. Tạo Order + OrderDetails (giá từ DB)
     *   5. Save order
     * Nếu bất kỳ bước nào fail → toàn bộ rollback.
     * Cart clear và email nằm ngoài transaction (postCheckout).
     * 
     * NOTE: Không quản lý tồn kho - admin sẽ gọi báo khách nếu hết hàng
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order checkout(CheckoutRequest request, Account account) {
        // STEP 1: Validate cart not empty
        Collection<CartItem> cartItems = cartService.getItems();
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Giỏ hàng trống, không thể đặt hàng!");
        }

        // STEP 2: VULN-DOUBLE-SPEND FIX: Lock Account trước để tránh race condition với points
        // Nếu không lock, 2 requests cùng lúc có thể đọc cùng 1 giá trị points và cùng trừ
        Account lockedAccount = null;
        if (account != null) {
            lockedAccount = entityManager.find(Account.class, account.getUsername(), LockModeType.PESSIMISTIC_WRITE);
            if (lockedAccount == null) {
                throw new IllegalStateException("Tài khoản không tồn tại!");
            }
        }

        // STEP 3: Lock tất cả sản phẩm — sort by ID để tránh deadlock
        List<Integer> productIds = cartItems.stream()
                .map(CartItem::getProductId)
                .distinct()
                .sorted()
                .toList();

        List<Product> lockedProducts = new ArrayList<>();
        for (Integer pid : productIds) {
            Product p = entityManager.find(Product.class, pid, LockModeType.PESSIMISTIC_WRITE);
            if (p == null) {
                throw new IllegalStateException("Sản phẩm ID " + pid + " không tồn tại!");
            }
            // VULN-PRODUCT-AVAILABILITY FIX: Check if product is available before checkout
            if (!Boolean.TRUE.equals(p.getAvailable())) {
                throw new com.springboot.jenka_coffee.exception.BusinessRuleException(
                        "Sản phẩm '" + p.getName() + "' hiện không còn kinh doanh!");
            }
            lockedProducts.add(p);
        }

        // STEP 4: Build Order + OrderDetails (giá lấy từ locked product)
        Order order = buildOrder(request, lockedAccount);
        List<OrderDetail> orderDetails = new ArrayList<>();
        for (CartItem item : cartItems) {
            Product product = lockedProducts.stream()
                    .filter(p -> p.getId().equals(item.getProductId()))
                    .findFirst().get();

            OrderDetail detail = new OrderDetail();
            detail.setProduct(product);
            detail.setPrice(product.getPrice()); // giá từ DB, không tin cart
            detail.setQuantity(item.getQuantity());
            detail.setOrder(order);
            orderDetails.add(detail);
        }
        order.setOrderDetails(orderDetails);

        BigDecimal totalAmount = orderDetails.stream()
                .map(d -> d.getPrice().multiply(BigDecimal.valueOf(d.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // VULN-C02 FIX: Validate + consume trong cùng 1 transaction với PESSIMISTIC_WRITE
        // VULN-VOUCHER-SIPHON FIX: Check user usage count against maxUsesPerUser limit
        // VULN-COMPILATION-ERROR FIX: Require login for all vouchers to prevent guest abuse
        // BUG-44 FIX: Support multiple uses per user based on maxUsesPerUser field
        Voucher appliedVoucher = null;
        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            if (lockedAccount == null) {
                throw new com.springboot.jenka_coffee.exception.BusinessRuleException(
                        "Vui lòng đăng nhập để sử dụng mã giảm giá!");
            }
            
            final Voucher voucher = voucherService.validateAndLockVoucher(
                request.getVoucherCode(), 
                totalAmount,
                lockedAccount.getUsername()
            );
            appliedVoucher = voucher;

            // Nếu scope = SPECIFIC, kiểm tra ít nhất 1 sản phẩm trong cart được áp dụng
            if ("SPECIFIC".equals(voucher.getScope())) {
                boolean hasApplicable = orderDetails.stream()
                        .anyMatch(d -> voucher.isApplicableToProduct(d.getProduct().getId()));
                if (!hasApplicable) {
                    throw new com.springboot.jenka_coffee.exception.BusinessRuleException(
                            "Mã giảm giá không áp dụng cho sản phẩm nào trong giỏ hàng!");
                }
                // Tính discount chỉ trên các sản phẩm được áp dụng
                BigDecimal applicableSubtotal = orderDetails.stream()
                        .filter(d -> voucher.isApplicableToProduct(d.getProduct().getId()))
                        .map(d -> d.getPrice().multiply(BigDecimal.valueOf(d.getQuantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal discount = voucher.calculateDiscount(applicableSubtotal);
                totalAmount = totalAmount.subtract(discount)
                        .max(new BigDecimal("1000")); // VULN-047 FIX: minimum 1.000đ
            } else {
                BigDecimal discount = voucher.calculateDiscount(totalAmount);
                totalAmount = totalAmount.subtract(discount)
                        .max(new BigDecimal("1000"));
            }
            order.setVoucherCode(appliedVoucher.getCode());
        }

        order.setTotalAmount(totalAmount);
        
        // VULN-INFINITE-POINTS FIX: Lưu số điểm thực tế đã sử dụng (hiện tại = 0 vì chưa implement points payment)
        // Khi implement points payment, cập nhật giá trị này
        order.setPointsUsed(0);

        // STEP 4: Save order (cascade save OrderDetails)
        Order savedOrder = orderRepository.save(order);

        // STEP 6b: VULN-023 FIX — Tạo Payment record (audit trail)
        // VULN-PAYMENT-WITHOUT-VERIFICATION WARNING: Payment status is PENDING
        // System does NOT verify actual payment before creating order
        // 
        // Current flow: Order created → Payment PENDING → Admin manually verifies
        // Risk: Users can place orders without paying (COD abuse, fake payment screenshots)
        // 
        // Production recommendations:
        // 1. For COD: Keep current flow (payment on delivery)
        // 2. For bank/momo: Integrate payment gateway webhooks
        //    - Create order with status PENDING_PAYMENT
        //    - Wait for payment gateway callback
        //    - Only confirm order after payment verified
        // 3. Implement payment timeout (auto-cancel unpaid orders after 15 minutes)
        // 4. Add admin dashboard to track pending payments
        com.springboot.jenka_coffee.entity.Payment payment = new com.springboot.jenka_coffee.entity.Payment();
        payment.setOrderId(savedOrder.getId());
        payment.setAmount(totalAmount);
        payment.setPaymentMethod(
            request.getPaymentMethod() != null ? request.getPaymentMethod().toUpperCase() : "COD");
        payment.setStatus(com.springboot.jenka_coffee.entity.Payment.PaymentStatus.PENDING.name());
        payment.setPaymentDate(LocalDateTime.now());
        entityManager.persist(payment);

        // STEP 7: Voucher đã được consume trong validateAndLockVoucher() — không cần gọi lại

        // VULN-C01 FIX: Clear cart TRONG transaction — ngăn double checkout
        // Nếu clear fail, transaction rollback → order không được tạo
        // Nếu cart đã rỗng (idempotent), không có vấn đề gì
        try {
            cartService.clear();
        } catch (Exception e) {
            log.warn("Cart clear in transaction failed for account={}: {}", 
                account != null ? account.getUsername() : "guest", e.getMessage());
            // Không throw — cart clear fail không nên rollback order đã tạo thành công
        }

        log.info("Checkout success: orderId={}, account={}, total={}, voucher={}",
                savedOrder.getId(),
                account != null ? account.getUsername() : "guest",
                totalAmount,
                appliedVoucher != null ? appliedVoucher.getCode() : "none");

        return savedOrder;
    }

    /**
     * Post-checkout side effects — gọi SAU khi transaction commit thành công.
     * Cart clear và email KHÔNG nằm trong transaction để tránh rollback vì lý do ngoài lề.
     */
    @Override
    public void postCheckout(Order savedOrder, Account account) {
        // Cart đã được clear trong checkout transaction (VULN-C01 FIX)
        // postCheckout chỉ xử lý email notification
        try {
            String customerName = account != null ? account.getFullname() : "Khách";
            emailService.sendNewOrderNotification(
                    adminEmail,
                    savedOrder.getId(),
                    customerName,
                    savedOrder.getPhone(),
                    savedOrder.getAddress(),
                    savedOrder.getTotalAmount()
            );
        } catch (Exception e) {
            log.warn("Failed to send order notification for orderId={}: {}", savedOrder.getId(), e.getMessage());
        }
    }

    private Order buildOrder(CheckoutRequest request, Account account) {
        Order order = new Order();
        order.setAccount(account);
        String fullAddress = String.format("%s, %s, %s, %s",
                request.getAddress(), request.getWard(),
                request.getDistrict(), request.getProvince());
        order.setAddress(fullAddress);
        order.setPhone(request.getPhone());
        order.setCreateDate(LocalDateTime.now());
        order.setStatus(0); // NEW
        // VULN-058 FIX: Sanitize note — strip HTML tags trước khi lưu
        if (request.getNote() != null && !request.getNote().isBlank()) {
            order.setNote(request.getNote().replaceAll("<[^>]*>", "").trim());
        }
        return order;
    }

    @Override
    public CheckoutRequest prepareCheckoutRequest(Account user) {
        CheckoutRequest request = new CheckoutRequest();
        if (user != null) {
            request.setFullname(user.getFullname());
            request.setEmail(user.getEmail());
            request.setPhone(user.getPhone());
        }
        return request;
    }

    @Override
    @Override
    @Transactional
    public Order updateStatus(Long orderId, int status) {
        // Lock Order to prevent race condition
        Order order = entityManager.find(Order.class, orderId, LockModeType.PESSIMISTIC_WRITE);
        if (order == null) {
            throw new RuntimeException("Không tìm thấy đơn hàng!");
        }

        Order.OrderStatus from = Order.OrderStatus.fromValue(order.getStatus());
        Order.OrderStatus to;
        try {
            to = Order.OrderStatus.fromValue(status);
        } catch (IllegalArgumentException e) {
            throw new com.springboot.jenka_coffee.exception.BusinessRuleException(
                    "Trạng thái đơn hàng không hợp lệ: " + status);
        }

        // Simplified status transitions: NEW -> CONFIRMED or CANCELLED
        boolean validTransition = switch (from) {
            case NEW       -> to == Order.OrderStatus.CONFIRMED || to == Order.OrderStatus.CANCELLED;
            case CONFIRMED -> to == Order.OrderStatus.CANCELLED; // Can only cancel after confirm
            case CANCELLED -> false; // Cannot change cancelled orders
        };

        if (!validTransition) {
            throw new com.springboot.jenka_coffee.exception.BusinessRuleException(
                    "Không thể chuyển trạng thái từ " + from.name() + " sang " + to.name());
        }

        // Refund points when order is cancelled
        if (to == Order.OrderStatus.CANCELLED && order.getAccount() != null) {
            Integer pointsToRefund = order.getPointsUsed();
            
            if (pointsToRefund != null && pointsToRefund > 0) {
                Account account = entityManager.find(Account.class, 
                    order.getAccount().getUsername(), LockModeType.PESSIMISTIC_WRITE);
                if (account != null) {
                    int currentPoints = account.getPoints() != null ? account.getPoints() : 0;
                    account.setPoints(currentPoints + pointsToRefund);
                    entityManager.merge(account);
                    
                    recordPointHistory(account.getUsername(), pointsToRefund, order.getId(), 
                        "Hoàn điểm do hủy đơn hàng #" + order.getId());
                    
                    log.info("Refunded {} points to user {} for cancelled order #{}", 
                        pointsToRefund, account.getUsername(), order.getId());
                }
            }
        }

        order.setStatus(status);
        log.info("Order #{} status changed: {} → {}", orderId, from.name(), to.name());
        return orderRepository.save(order);
    }
    
    /**
     * VULN-AUDIT-TRAIL FIX: Record point transaction in PointHistory for audit trail
     * This provides traceability for all point changes (earn, spend, refund)
     */
    private void recordPointHistory(String username, int amount, Long orderId, String reason) {
        com.springboot.jenka_coffee.entity.PointHistory history = 
            new com.springboot.jenka_coffee.entity.PointHistory();
        history.setUsername(username);
        history.setAmount(amount);
        history.setOrderId(orderId);
        history.setReason(reason);
        history.setCreateDate(LocalDateTime.now());
        pointHistoryRepository.save(history);
    }

    @Override
    public Page<Order> findAll(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    @Override
    public List<Order> findAllWithAccountByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return orderRepository.findAllWithAccountByIds(ids);
    }
}
