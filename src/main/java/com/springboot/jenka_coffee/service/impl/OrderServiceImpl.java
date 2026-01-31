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
import com.springboot.jenka_coffee.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;

    public OrderServiceImpl(OrderRepository orderRepository,
            ProductRepository productRepository,
            CartService cartService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.cartService = cartService;
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
    public List<Order> findByUsername(String username) {
        // Cách 1: Query trong DAO (List<Order> findByAccount_Username(String username))
        // return orderRepository.findByAccount_Username(username);

        // Cách 2: Stream lọc (Nếu DAO chưa viết hàm tìm kiếm)
        return orderRepository.findAll().stream()
                .filter(o -> o.getAccount().getUsername().equals(username))
                .collect(Collectors.toList());
    }

    /**
     * CHECKOUT TRANSACTION
     * Steps: 1. Validate cart → 2. Create Order+Details → 3. Deduct inventory → 4.
     * Clear cart
     */
    @Override
    @Transactional
    public Order checkout(CheckoutRequest request) {
        // STEP 1: Validate cart not empty
        Collection<CartItem> cartItems = cartService.getItems();
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Giỏ hàng trống, không thể đặt hàng!");
        }

        // STEP 2: Create Order + OrderDetails
        Order order = buildOrder(request);
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

        return savedOrder;
    }

    /**
     * Build Order from CheckoutRequest
     */
    private Order buildOrder(CheckoutRequest request) {
        Order order = new Order();

        // TODO: Get authenticated user from SecurityContext
        // For now, hardcode or use request data
        Account account = new Account();
        account.setUsername(request.getFullname()); // Temporary - should be from session
        order.setAccount(account);

        // Build full address
        String fullAddress = String.format("%s, %s, %s, %s",
                request.getAddress(),
                request.getWard(),
                request.getDistrict(),
                request.getProvince());
        order.setAddress(fullAddress);
        order.setPhone(request.getPhone());
        order.setCreateDate(LocalDateTime.now());
        order.setStatus(0); // Status: NEW

        return order;
    }

    /**
     * Build OrderDetails from cart items
     */
    private List<OrderDetail> buildOrderDetails(Collection<CartItem> cartItems, Order order) {
        List<OrderDetail> orderDetails = new ArrayList<>();

        for (CartItem item : cartItems) {
            OrderDetail detail = new OrderDetail();

            // Fetch product from database to ensure data accuracy
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Sản phẩm ID " + item.getProductId() + " không tồn tại!"));

            detail.setProduct(product);
            detail.setPrice(item.getPrice());
            detail.setQuantity(item.getQuantity());
            detail.setOrder(order);

            orderDetails.add(detail);
        }

        return orderDetails;
    }

    /**
     * Deduct inventory from products
     * Validates stock availability before deducting
     * Throws InsufficientStockException if stock unavailable
     */
    private void deductInventory(List<OrderDetail> orderDetails) {
        for (OrderDetail detail : orderDetails) {
            Product product = detail.getProduct();
            Integer requestedQty = detail.getQuantity();
            Integer currentStock = product.getQuantity();

            // Validate stock availability
            if (currentStock < requestedQty) {
                throw new InsufficientStockException(
                        product.getName(),
                        requestedQty,
                        currentStock);
            }

            // Deduct stock
            product.setQuantity(currentStock - requestedQty);
            productRepository.save(product);
        }
    }
}