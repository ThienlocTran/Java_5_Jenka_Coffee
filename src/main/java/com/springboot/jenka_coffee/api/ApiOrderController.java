package com.springboot.jenka_coffee.api;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.dto.request.CheckoutRequest;
import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.entity.Order;
import com.springboot.jenka_coffee.service.AccountService;
import com.springboot.jenka_coffee.service.CartService;
import com.springboot.jenka_coffee.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class ApiOrderController {

    private final CartService cartService;
    private final OrderService orderService;
    private final AccountService accountService;

    public ApiOrderController(CartService cartService, OrderService orderService, AccountService accountService) {
        this.cartService = cartService;
        this.orderService = orderService;
        this.accountService = accountService;
    }

    @GetMapping("/checkout-info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCheckoutInfo(
            @AuthenticationPrincipal String username) {
        if (cartService.getItems().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Giỏ hàng trống"));
        }
        Account user = accountService.findById(username);
        CheckoutRequest request = orderService.prepareCheckoutRequest(user);

        Map<String, Object> data = new HashMap<>();
        data.put("checkoutRequest", request);
        data.put("cartItems",  cartService.getItems());
        data.put("cartTotal",  cartService.getTotal());
        data.put("cartCount",  cartService.getCount());
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin thanh toán thành công", data));
    }

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<Map<String, Object>>> processCheckout(
            @Valid @RequestBody CheckoutRequest request,
            @AuthenticationPrincipal String username) {
        Account user = accountService.findById(username);
        Order order = orderService.checkout(request, user);
        orderService.postCheckout(order, user);
        return ResponseEntity.ok(ApiResponse.success(
                "Đặt hàng thành công! Mã đơn hàng: #" + order.getId(),
                Map.of("orderId", order.getId())));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrderHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @AuthenticationPrincipal String username) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 20), Sort.by("id").descending());
        Page<Order> orderPage = orderService.findByUsername(username, pageable);

        Map<String, Object> data = new HashMap<>();
        data.put("items",       orderPage.getContent());
        data.put("currentPage", orderPage.getNumber());
        data.put("totalPages",  orderPage.getTotalPages());
        data.put("totalItems",  orderPage.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử đơn hàng thành công", data));
    }
}
