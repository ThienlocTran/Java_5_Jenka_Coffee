package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.dto.request.CheckoutRequest;
import com.springboot.jenka_coffee.dto.response.CartItem;
import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.entity.Order;
import com.springboot.jenka_coffee.entity.OrderDetail;
import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.service.CartService;
import com.springboot.jenka_coffee.service.CheckoutService;
import com.springboot.jenka_coffee.service.OrderService;
import com.springboot.jenka_coffee.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class CheckoutServiceImpl implements CheckoutService {

    private final CartService cartService;
    private final OrderService orderService;
    private final ProductService productService;

    public CheckoutServiceImpl(CartService cartService, OrderService orderService, ProductService productService) {
        this.cartService = cartService;
        this.orderService = orderService;
        this.productService = productService;
    }

    @Override
    @Transactional
    public Order processCheckout(CheckoutRequest request) {
        // Kiểm tra giỏ hàng trống
        Collection<CartItem> items = cartService.getItems();
        if (items.isEmpty()) {
            throw new IllegalStateException("Giỏ hàng trống, không thể đặt hàng!");
        }

        // Tạo Order
        Order order = buildOrder(request);

        // Tạo OrderDetails từ giỏ hàng
        List<OrderDetail> orderDetails = buildOrderDetails(items, order);
        order.setOrderDetails(orderDetails);

        // Lưu order vào database
        Order savedOrder = orderService.create(order);

        // Xóa giỏ hàng sau khi đặt hàng thành công
        cartService.clear();

        return savedOrder;
    }

    @Override
    public boolean isCartEmpty() {
        return cartService.getItems().isEmpty();
    }

    /**
     * Xây dựng Order từ CheckoutRequest
     */
    private Order buildOrder(CheckoutRequest request) {
        Order order = new Order();

        // TODO: Lấy user từ session khi có authentication
        // Tạm thời dùng account mặc định
        Account account = new Account();
        account.setUsername("user"); // Hardcode tạm, sau này lấy từ SecurityContext
        order.setAccount(account);

        // Ghép địa chỉ đầy đủ
        String fullAddress = buildFullAddress(request);
        order.setAddress(fullAddress);
        order.setPhone(request.getPhone());
        order.setCreateDate(LocalDateTime.now());
        order.setStatus(0); // Trạng thái: Mới đặt

        return order;
    }

    /**
     * Ghép địa chỉ đầy đủ từ các trường
     */
    private String buildFullAddress(CheckoutRequest request) {
        return String.format("%s, %s, %s, %s",
                request.getAddress(),
                request.getWard(),
                request.getDistrict(),
                request.getProvince());
    }

    /**
     * Xây dựng danh sách OrderDetail từ giỏ hàng
     */
    private List<OrderDetail> buildOrderDetails(Collection<CartItem> items, Order order) {
        List<OrderDetail> orderDetails = new ArrayList<>();

        for (CartItem item : items) {
            OrderDetail detail = new OrderDetail();

            // Lấy product từ database để đảm bảo dữ liệu chính xác
            Product product = productService.findById(item.getProductId());
            if (product == null) {
                throw new IllegalStateException("Sản phẩm ID " + item.getProductId() + " không tồn tại!");
            }

            detail.setProduct(product);
            detail.setPrice(item.getPrice());
            detail.setQuantity(item.getQuantity());
            detail.setOrder(order);

            orderDetails.add(detail);
        }

        return orderDetails;
    }
}
