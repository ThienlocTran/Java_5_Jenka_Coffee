package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.dto.request.CheckoutRequest;
import com.springboot.jenka_coffee.dto.response.CartItem;
import com.springboot.jenka_coffee.entity.*;
import com.springboot.jenka_coffee.exception.BusinessRuleException;
import com.springboot.jenka_coffee.exception.ResourceNotFoundException;
import com.springboot.jenka_coffee.repository.OrderRepository;
import com.springboot.jenka_coffee.repository.PointHistoryRepository;
import com.springboot.jenka_coffee.service.CartService;
import com.springboot.jenka_coffee.service.EmailService;
import com.springboot.jenka_coffee.service.OrderService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final EmailService emailService;
    private final EntityManager entityManager;
    private final PointHistoryRepository pointHistoryRepository;

    @Value("${spring.mail.username}")
    private String adminEmail;

    public OrderServiceImpl(OrderRepository orderRepository,
                            CartService cartService,
                            EmailService emailService,
                            EntityManager entityManager,
                            PointHistoryRepository pointHistoryRepository) {
        this.orderRepository = orderRepository;
        this.cartService = cartService;
        this.emailService = emailService;
        this.entityManager = entityManager;
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
                .orElseThrow(() -> new ResourceNotFoundException(
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
        Account lockedAccount = lockAccount(account);

        // STEP 3: Lock tất cả sản phẩm — sort by ID để tránh deadlock
        Map<Integer, Product> productMap = lockProducts(cartItems);

        // STEP 4: Build Order + OrderDetails (giá lấy từ locked product)
        Order order = buildOrder(request, lockedAccount);
        List<OrderDetail> orderDetails = buildOrderDetails(cartItems, productMap, order);
        order.setOrderDetails(orderDetails);

        BigDecimal totalAmount = orderDetails.stream()
                .map(d -> d.getPrice().multiply(BigDecimal.valueOf(d.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // VULN-H02 FIX: Validate max order value to prevent abuse
        // Maximum order value: 500 million VND (reasonable for B2C coffee equipment)
        BigDecimal MAX_ORDER_VALUE = new BigDecimal("500000000"); // 500M VND
        if (totalAmount.compareTo(MAX_ORDER_VALUE) > 0) {
            throw new com.springboot.jenka_coffee.exception.BusinessRuleException(
                    "Giá trị đơn hàng vượt quá giới hạn cho phép (" + 
                    MAX_ORDER_VALUE.divide(new BigDecimal("1000000")).intValue() + " triệu VNĐ). " +
                    "Vui lòng liên hệ trực tiếp để được hỗ trợ đặt hàng số lượng lớn.");
        }

        // VULN-C02 FIX: Validate + consume trong cùng 1 transaction với PESSIMISTIC_WRITE
        // VULN-VOUCHER-SIPHON FIX: Check user usage count against maxUsesPerUser limit
        // VULN-COMPILATION-ERROR FIX: Require login for all vouchers to prevent guest abuse
        // BUG-44 FIX: Support multiple uses per user based on maxUsesPerUser field

        order.setTotalAmount(totalAmount);

        // VULN-INFINITE-POINTS FIX: Lưu số điểm thực tế đã sử dụng
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
        createPayment(savedOrder, totalAmount, request);

        // STEP 7: Voucher đã được consume trong validateAndLockVoucher() — không cần gọi lại

        // VULN-C01 FIX: Clear cart TRONG transaction — ngăn double checkout
        try {
            cartService.clear();
        } catch (Exception e) {
            log.warn("Cart clear in transaction failed for account={}: {}",
                    account != null ? account.getUsername() : "guest", e.getMessage());
        }

        log.info("Checkout success: orderId={}, account={}, total={}",
                savedOrder.getId(),
                account != null ? account.getUsername() : "guest",
                totalAmount);

        return savedOrder;
    }

    private Account lockAccount(Account account) {
        if (account == null) return null;

        Account locked = entityManager.find(Account.class,
                account.getUsername(),
                LockModeType.PESSIMISTIC_WRITE);

        if (locked == null) {
            throw new IllegalStateException("Tài khoản không tồn tại!");
        }
        return locked;
    }

    private Map<Integer, Product> lockProducts(Collection<CartItem> cartItems) {
        List<Integer> productIds = cartItems.stream()
                .map(CartItem::getProductId)
                .distinct()
                .sorted()
                .toList();

        Map<Integer, Product> map = new HashMap<>();

        for (Integer pid : productIds) {
            Product p = entityManager.find(Product.class, pid, LockModeType.PESSIMISTIC_WRITE);

            if (p == null) {
                throw new IllegalStateException("Sản phẩm ID " + pid + " không tồn tại!");
            }

            // VULN-PRODUCT-AVAILABILITY FIX: Check if product is available before checkout
            if (!Boolean.TRUE.equals(p.getAvailable())) {
                throw new BusinessRuleException(
                        "Sản phẩm '" + p.getName() + "' hiện không còn kinh doanh!");
            }

            map.put(pid, p);
        }

        return map;
    }

    private List<OrderDetail> buildOrderDetails(Collection<CartItem> cartItems,
                                                Map<Integer, Product> productMap,
                                                Order order) {

        List<OrderDetail> orderDetails = new ArrayList<>();

        for (CartItem item : cartItems) {
            Product product = productMap.get(item.getProductId());

            if (product == null) {
                throw new IllegalStateException("Product not found: " + item.getProductId());
            }

            OrderDetail detail = new OrderDetail();
            detail.setProduct(product);
            detail.setPrice(product.getPrice()); // giá từ DB, không tin cart
            detail.setQuantity(item.getQuantity());
            detail.setOrder(order);

            orderDetails.add(detail);
        }

        return orderDetails;
    }

    private void createPayment(Order savedOrder, BigDecimal totalAmount, CheckoutRequest request) {
        Payment payment = new Payment();
        payment.setOrderId(savedOrder.getId());
        payment.setAmount(totalAmount);
        payment.setPaymentMethod(
                request.getPaymentMethod() != null
                        ? request.getPaymentMethod().toUpperCase()
                        : "COD");
        payment.setStatus(Payment.PaymentStatus.PENDING.name());
        payment.setPaymentDate(LocalDateTime.now());

        entityManager.persist(payment);
    }

    /**
     * Post-checkout side effects — gọi SAU khi transaction commit thành công.
     */
    @Override
    public void postCheckout(Order savedOrder, Account account) {
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
            log.warn("Failed to send order notification for orderId={}: {}",
                    savedOrder.getId(), e.getMessage());
        }
    }

    private Order buildOrder(CheckoutRequest request, Account account) {
        Order order = new Order();
        order.setAccount(account);

        String fullAddress = String.format("%s, %s, %s, %s",
                request.getAddress(),
                request.getWard(),
                request.getDistrict(),
                request.getProvince());

        order.setAddress(fullAddress);
        order.setPhone(request.getPhone());
        order.setCreateDate(LocalDateTime.now());
        order.setStatus(0); // NEW

        // VULN #12 FIX: XSS Prevention in Order Notes
        // PROBLEM: Regex sanitization <[^>]*> is insufficient
        // - Doesn't match incomplete tags: <img src=x onerror=alert(1)
        // - Regex is NOT a proper HTML sanitizer
        // SOLUTION: Use proper HTML escaping (converts < to &lt;, > to &gt;, etc.)
        // - If frontend uses v-html, browser won't execute escaped HTML as code
        // - Simple, safe, and effective for plain text fields
        if (request.getNote() != null && !request.getNote().isBlank()) {
            String sanitizedNote = org.springframework.web.util.HtmlUtils.htmlEscape(request.getNote().trim());
            order.setNote(sanitizedNote);
            
            // Log if potential XSS attempt detected
            if (!sanitizedNote.equals(request.getNote().trim())) {
                log.warn("SECURITY: Potential XSS attempt in order note from account={}, original length={}, escaped length={}",
                        account != null ? account.getUsername() : "guest",
                        request.getNote().length(),
                        sanitizedNote.length());
            }
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
    @Transactional
    public void updateStatus(Long orderId, int status) {

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
            throw new BusinessRuleException("Trạng thái đơn hàng không hợp lệ: " + status);
        }

        boolean validTransition = switch (from) {
            case NEW -> to == Order.OrderStatus.CONFIRMED || to == Order.OrderStatus.CANCELLED;
            case CONFIRMED -> to == Order.OrderStatus.CANCELLED;
            case CANCELLED -> false;
        };

        if (!validTransition) {
            throw new BusinessRuleException(
                    "Không thể chuyển trạng thái từ " + from.name() + " sang " + to.name());
        }

        // Refund points when order is cancelled
        if (to == Order.OrderStatus.CANCELLED && order.getAccount() != null) {

            Integer pointsToRefund = order.getPointsUsed();

            if (pointsToRefund != null && pointsToRefund > 0) {

                Account account = entityManager.find(Account.class,
                        order.getAccount().getUsername(),
                        LockModeType.PESSIMISTIC_WRITE);

                if (account != null) {
                    int currentPoints = Optional.ofNullable(account.getPoints()).orElse(0);
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
        orderRepository.save(order);
    }

    /**
     * VULN-AUDIT-TRAIL FIX: Record point transaction in PointHistory for audit trail
     */
    private void recordPointHistory(String username, int amount, Long orderId, String reason) {
        PointHistory history = new PointHistory();
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