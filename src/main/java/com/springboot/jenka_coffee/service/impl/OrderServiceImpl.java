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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;
    private final EmailService emailService;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.username}")
    private String adminEmail;

    public OrderServiceImpl(OrderRepository orderRepository,
                            ProductRepository productRepository,
                            CartService cartService,
                            EmailService emailService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.cartService = cartService;
        this.emailService = emailService;
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
        // SỬA: Thêm tham số "pageable" vào cuối
        return orderRepository.findByAccount_Username(username, pageable);
    }

    /**
     * CHECKOUT TRANSACTION
     * Steps: 1. Validate cart → 2. Create Order+Details → 3. Deduct inventory → 4. Clear cart
     */
    @Override
    @Transactional
    // 1. Đã thêm tham số Account account
    public Order checkout(CheckoutRequest request, Account account) {
        // STEP 1: Validate cart not empty
        Collection<CartItem> cartItems = cartService.getItems();
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Giỏ hàng trống, không thể đặt hàng!");
        }

        // STEP 2: Create Order + OrderDetails
        // Truyền account vào hàm buildOrder
        Order order = buildOrder(request, account);
        List<OrderDetail> orderDetails = buildOrderDetails(cartItems, order);
        order.setOrderDetails(orderDetails);

        // Calculate total amount
        BigDecimal totalAmount = orderDetails.stream()
                .map(detail -> detail.getPrice().multiply(BigDecimal.valueOf(detail.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(totalAmount);

        // STEP 3: Deduct inventory (with validation)
        deductInventory(orderDetails);

        // Save order to database
        Order savedOrder = orderRepository.save(order);

        // STEP 4: Clear cart after successful transaction
        cartService.clear();

        // STEP 5: Notify admin via email (async — không block response)
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
            // Không để lỗi mail block đơn hàng đã tạo thành công
        }

        return savedOrder;
    }

    /**
     * Build Order from CheckoutRequest
     */
    private Order buildOrder(CheckoutRequest request, Account account) {
        Order order = new Order();

        // 2. QUAN TRỌNG: Gán tài khoản thật vào đơn hàng (Thay vì tạo new Account rỗng như cũ)
        order.setAccount(account);

        // Build full address (Ghép chuỗi địa chỉ đầy đủ)
        String fullAddress = String.format("%s, %s, %s, %s",
                request.getAddress(),
                request.getWard(),
                request.getDistrict(),
                request.getProvince());

        order.setAddress(fullAddress);
        order.setPhone(request.getPhone());

        // Lưu ý: Nếu Entity Order dùng java.util.Date thì sửa thành new Date()
        // Nếu dùng LocalDateTime thì giữ nguyên dòng dưới
        order.setCreateDate(LocalDateTime.now());

        order.setStatus(0); // Status: NEW (Chờ xác nhận)

        return order;
    }

    /**
     * Build OrderDetails — price luôn lấy từ DB, không tin cart session
     */
    private List<OrderDetail> buildOrderDetails(Collection<CartItem> cartItems, Order order) {
        List<OrderDetail> orderDetails = new ArrayList<>();

        for (CartItem item : cartItems) {
            // Fetch product từ DB để lấy giá thật (tránh price tampering từ client)
            Product product = productRepository.findByIdWithCategory(item.getProductId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Sản phẩm ID " + item.getProductId() + " không tồn tại!"));

            OrderDetail detail = new OrderDetail();
            detail.setProduct(product);
            detail.setPrice(product.getPrice()); // giá từ DB, không từ cart
            detail.setQuantity(item.getQuantity());
            detail.setOrder(order);
            orderDetails.add(detail);
        }

        return orderDetails;
    }

    /**
     * Validate stock và deduct inventory — validate tất cả trước, rồi mới save batch
     */
    private void deductInventory(List<OrderDetail> orderDetails) {
        // Pass 1: validate tất cả trước để tránh partial deduction
        for (OrderDetail detail : orderDetails) {
            Product product = detail.getProduct();
            int requested = detail.getQuantity();
            int current = product.getQuantity() != null ? product.getQuantity() : 0;
            if (current < requested) {
                throw new InsufficientStockException(product.getName(), requested, current);
            }
        }
        // Pass 2: deduct và batch save
        List<Product> toSave = new ArrayList<>();
        for (OrderDetail detail : orderDetails) {
            Product product = detail.getProduct();
            product.setQuantity(product.getQuantity() - detail.getQuantity());
            toSave.add(product);
        }
        productRepository.saveAll(toSave);
    }

    @Override
    public CheckoutRequest prepareCheckoutRequest(Account user) {
        CheckoutRequest request = new CheckoutRequest();

        if (user != null) {
            // Auto-fill with user data for logged-in users
            request.setFullname(user.getFullname());
            request.setEmail(user.getEmail());
            request.setPhone(user.getPhone());
        }

        return request;
    }

    @Override
    public Order updateStatus(Long orderId, int status) {
        // Validate status value via enum — throws IllegalArgumentException for invalid values
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