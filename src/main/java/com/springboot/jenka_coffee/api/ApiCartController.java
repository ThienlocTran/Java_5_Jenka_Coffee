package com.springboot.jenka_coffee.api;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.service.CartService;
import com.springboot.jenka_coffee.service.impl.CartServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/cart")
public class ApiCartController {

    private final CartService cartService;

    public ApiCartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCart(
            @AuthenticationPrincipal String username,
            HttpServletRequest request) {
        try {
            CartServiceImpl.setRequestContext(request);
            log.debug("[CART GET] user={}, size={}", username, cartService.getCount());
            Map<String, Object> data = new HashMap<>();
            data.put("items",       cartService.getItems());
            data.put("totalAmount", cartService.getTotal());
            data.put("summary",     cartService.getCartSummary());
            return ResponseEntity.ok(ApiResponse.success("Lấy thông tin giỏ hàng thành công", data));
        } finally {
            CartServiceImpl.clearRequestContext();
        }
    }

    @PostMapping("/add/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addToCart(
            @PathVariable("id") Integer id,
            @AuthenticationPrincipal String username,
            HttpServletRequest request) {
        try {
            CartServiceImpl.setRequestContext(request);
            log.debug("[CART ADD] user={}, productId={}", username, id);
            cartService.add(id);
            Map<String, Object> data = new HashMap<>();
            data.put("summary", cartService.getCartSummary());
            return ResponseEntity.ok(ApiResponse.success("Đã thêm sản phẩm vào giỏ hàng", data));
        } finally {
            CartServiceImpl.clearRequestContext();
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateCartItem(
            @PathVariable("id") Integer id,
            @RequestParam("qty") int qty,
            HttpServletRequest request) {
        try {
            CartServiceImpl.setRequestContext(request);
            cartService.update(id, qty);
            Map<String, Object> data = new HashMap<>();
            data.put("items",       cartService.getItems());
            data.put("totalAmount", cartService.getTotal());
            return ResponseEntity.ok(ApiResponse.success("Cập nhật số lượng thành công", data));
        } finally {
            CartServiceImpl.clearRequestContext();
        }
    }

    @DeleteMapping("/remove/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> removeFromCart(
            @PathVariable("id") Integer id,
            HttpServletRequest request) {
        try {
            CartServiceImpl.setRequestContext(request);
            cartService.remove(id);
            Map<String, Object> data = new HashMap<>();
            data.put("items",       cartService.getItems());
            data.put("totalAmount", cartService.getTotal());
            return ResponseEntity.ok(ApiResponse.success("Đã xóa sản phẩm khỏi giỏ hàng", data));
        } finally {
            CartServiceImpl.clearRequestContext();
        }
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<Void>> clearCart(HttpServletRequest request) {
        try {
            CartServiceImpl.setRequestContext(request);
            cartService.clear();
            return ResponseEntity.ok(ApiResponse.success("Đã xóa toàn bộ giỏ hàng", null));
        } finally {
            CartServiceImpl.clearRequestContext();
        }
    }
}
