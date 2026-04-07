package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.dto.request.CheckoutRequest;
import com.springboot.jenka_coffee.dto.response.CartItem;
import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.entity.Order;
import com.springboot.jenka_coffee.entity.OrderDetail;
import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.exception.InsufficientStockException;
import com.springboot.jenka_coffee.repository.OrderRepository;
import com.springboot.jenka_coffee.repository.ProductRepository;
import com.springboot.jenka_coffee.service.CartService;
import com.springboot.jenka_coffee.service.EmailService;
import com.springboot.jenka_coffee.service.OrderService;
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

import com.springboot.jenka_coffee.entity.Voucher;
import com.springboot.jenka_coffee.service.VoucherService;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;
    private final EmailService emailService;
    private final EntityManager entityManager;
    private final VoucherService voucherService;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.username}")
    private String adminEmail;

    public OrderServiceImpl(OrderRepository orderRepository,
                            ProductRepository productRepository,
                            CartService cartService,
                            EmailService emailService,
                            EntityManager entityManager,
                            VoucherService voucherService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.cartService = cartService;
        this.emailService = emailService;
        this.entityManager = entityManager;
        this.voucherService = voucherService;
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
    public Page<Order> findByUsername(String username, Pageable pageable) {
        return orderRepository.findByAccount_Username(username, pageable);
    }

    /**
     * CHECKOUT TRANSACTION — ACID guaranteed
     *
     * Luồng:
     *   1. Validate giỏ hàng không rỗng
     *   2. Lock sản phẩm (PESSIMISTIC_WRITE) để tránh race condition
     *   3. Validate tồn kho sau khi lock (fresh data)
     *   4. Tạo Order + OrderDetails (giá từ DB)
     *   5. Trừ tồn kho (batch update)
     *   6. Save order
     *
     * Nếu bất kỳ bước nào fail → toàn bộ rollback, tồn kho không bị trừ.
     * Cart clear và email nằm ngoài transaction (postCheckout).
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order checkout(CheckoutRequest request, Account account) {
        // STEP 1: Validate cart not empty
        Collection<CartItem> cartItems = cartService.getItems();
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Giỏ hàng trống, không thể đặt hàng!");
        }

        // STEP 2: Lock tất cả sản phẩm — sort by ID để tránh deadlock
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
            lockedProducts.add(p);
        }

        // STEP 3: Validate tồn kho sau khi đã lock (fresh, không stale)
        for (CartItem item : cartItems) {
            Product product = lockedProducts.stream()
                    .filter(p -> p.getId().equals(item.getProductId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Sản phẩm không tồn tại!"));

            int current = product.getQuantity() != null ? product.getQuantity() : 0;
            if (current < item.getQuantity()) {
                throw new InsufficientStockException(product.getName(), item.getQuantity(), current);
            }
        }

        // STEP 4: Build Order + OrderDetails (giá lấy từ locked product)
        Order order = buildOrder(request, account);
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

        // ── Áp dụng Voucher (nếu có) ────────────────────────────────────────
        Voucher appliedVoucher = null;
        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            appliedVoucher = voucherService.validateForCheckout(request.getVoucherCode(), totalAmount);

            // Nếu scope = SPECIFIC, kiểm tra ít nhất 1 sản phẩm trong cart được áp dụng
            if ("SPECIFIC".equals(appliedVoucher.getScope())) {
                boolean hasApplicable = orderDetails.stream()
                        .anyMatch(d -> appliedVoucher.isApplicableToProduct(d.getProduct().getId()));
                if (!hasApplicable) {
                    throw new com.springboot.jenka_coffee.exception.BusinessRuleException(
                            "Mã giảm giá không áp dụng cho sản phẩm nào trong giỏ hàng!");
                }
                // Tính discount chỉ trên các sản phẩm được áp dụng
                BigDecimal applicableSubtotal = orderDetails.stream()
                        .filter(d -> appliedVoucher.isApplicableToProduct(d.getProduct().getId()))
                        .map(d -> d.getPrice().multiply(BigDecimal.valueOf(d.getQuantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal discount = appliedVoucher.calculateDiscount(applicableSubtotal);
                totalAmount = totalAmount.subtract(discount).max(BigDecimal.ZERO);
            } else {
                BigDecimal discount = appliedVoucher.calculateDiscount(totalAmount);
                totalAmount = totalAmount.subtract(discount).max(BigDecimal.ZERO);
            }
            order.setVoucherCode(appliedVoucher.getCode());
        }

        order.setTotalAmount(totalAmount);

        // STEP 5: Trừ tồn kho trên locked entities — batch save
        for (OrderDetail detail : orderDetails) {
            Product product = detail.getProduct();
            product.setQuantity(product.getQuantity() - detail.getQuantity());
        }
        productRepository.saveAll(lockedProducts);

        // STEP 6: Save order (cascade save OrderDetails)
        Order savedOrder = orderRepository.save(order);

        // STEP 7: Trừ lượt dùng voucher (pessimistic lock trong VoucherService)
        if (appliedVoucher != null) {
            voucherService.consumeVoucher(appliedVoucher.getCode());
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
        try {
            cartService.clear();
        } catch (Exception e) {
            log.warn("Failed to clear cart after checkout orderId={}: {}", savedOrder.getId(), e.getMessage());
        }

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
    public Order updateStatus(Long orderId, int status) {
        Order.OrderStatus.fromValue(status);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng!"));
        order.setStatus(status);
        return orderRepository.save(order);
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
