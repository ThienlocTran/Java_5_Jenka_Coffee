package com.springboot.jenka_coffee.api;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.dto.request.CheckoutRequest;
import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.entity.Order;
import com.springboot.jenka_coffee.service.CartService;
import com.springboot.jenka_coffee.service.OrderService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class ApiOrderController {

    private final CartService cartService;
    private final OrderService orderService;

    public ApiOrderController(CartService cartService, OrderService orderService) {
        this.cartService = cartService;
        this.orderService = orderService;
    }

    @GetMapping("/checkout-info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCheckoutInfo(HttpSession session) {
        if (cartService.getItems().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("Giỏ hàng trống"));
        }

        Account user = (Account) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Vui lòng đăng nhập"));
        }

        CheckoutRequest request = orderService.prepareCheckoutRequest(user);

        Map<String, Object> data = new HashMap<>();
        data.put("checkoutRequest", request);
        data.put("cartItems", cartService.getItems());
        data.put("cartTotal", cartService.getTotal());
        data.put("cartCount", cartService.getCount());

        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin thanh toán thành công", data));
    }

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<Map<String, Object>>> processCheckout(
            @Valid @RequestBody CheckoutRequest request,
            HttpSession session) {

        Account user = (Account) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Vui lòng đăng nhập để thanh toán"));
        }

        Order order = orderService.checkout(request, user);
        // postCheckout chạy SAU khi transaction commit — cart clear + email không ảnh hưởng rollback
        orderService.postCheckout(order, user);
        Map<String, Object> data = Map.of("orderId", order.getId());
        return ResponseEntity.ok(ApiResponse.success("Đặt hàng thành công! Mã đơn hàng: #" + order.getId(), data));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrderHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            HttpSession session) {

        Account user = (Account) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Vui lòng đăng nhập"));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Order> orderPage = orderService.findByUsername(user.getUsername(), pageable);

        Map<String, Object> data = new HashMap<>();
        data.put("items", orderPage.getContent());
        data.put("currentPage", orderPage.getNumber());
        data.put("totalPages", orderPage.getTotalPages());
        data.put("totalItems", orderPage.getTotalElements());

        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử đơn hàng thành công", data));
    }
}
