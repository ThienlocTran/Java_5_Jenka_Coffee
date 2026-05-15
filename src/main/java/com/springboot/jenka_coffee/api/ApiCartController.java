package com.springboot.jenka_coffee.api;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.service.CartService;
import com.springboot.jenka_coffee.service.impl.CartServiceImpl;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/cart")
public class ApiCartController {

    private final CartService cartService;

    public ApiCartController(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * Resolve cart key:
     * - Authenticated: username (persistent forever in DB)
     * - Anonymous: 'anon:<uuid>' via SESSION cookie (lost when browser closes — by design)
     *
     * Business rules:
     * - Logged-in user cart NEVER expires → encourages purchase across sessions/devices
     * - Anonymous cart is session-only → no commitment, no persistence needed
     */
    private String resolveCartKey(String username, HttpServletRequest request, HttpServletResponse response) {
        if (username != null) return username;

        // Anonymous: look for cart_id session cookie
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("cart_id".equals(cookie.getName())) {
                    String val = cookie.getValue();
                    if (val != null && !val.isBlank()) {
                        return "anon:" + val;
                    }
                }
            }
        }

        // No cookie yet — generate new UUID cart ID
        String cartUuid = UUID.randomUUID().toString();
        Cookie cookie = new Cookie("cart_id", cartUuid);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);        // HTTPS only (Vietnix)
        cookie.setPath("/");
        // NOTE: DO NOT call setMaxAge() — this makes it a SESSION cookie.
        // Browser will delete it automatically when the window/tab is closed.
        // This is intentional: anonymous carts are temporary by design.
        // Logged-in users get persistent carts via username key in DB.
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);

        log.debug("[CART] New anonymous session-cart created: anon:{}", cartUuid);
        return "anon:" + cartUuid;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCart(
            @AuthenticationPrincipal String username,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            CartServiceImpl.setCartKey(resolveCartKey(username, request, response));
            log.debug("[CART GET] user={}, size={}", username, cartService.getCount());
            Map<String, Object> data = new HashMap<>();
            data.put("items",       cartService.getItems());
            data.put("totalAmount", cartService.getTotal());
            data.put("summary",     cartService.getCartSummary());
            return ResponseEntity.ok(ApiResponse.success("Lấy thông tin giỏ hàng thành công", data));
        } finally {
            CartServiceImpl.clearCartKey();
        }
    }

    @PostMapping("/add/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addToCart(
            @PathVariable("id") Integer id,
            @AuthenticationPrincipal String username,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            CartServiceImpl.setCartKey(resolveCartKey(username, request, response));
            log.debug("[CART ADD] user={}, productId={}", username, id);
            cartService.add(id);
            Map<String, Object> data = new HashMap<>();
            data.put("summary", cartService.getCartSummary());
            return ResponseEntity.ok(ApiResponse.success("Đã thêm sản phẩm vào giỏ hàng", data));
        } finally {
            CartServiceImpl.clearCartKey();
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateCartItem(
            @PathVariable("id") Integer id,
            @RequestParam("qty") int qty,
            @AuthenticationPrincipal String username,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            CartServiceImpl.setCartKey(resolveCartKey(username, request, response));
            cartService.update(id, qty);
            Map<String, Object> data = new HashMap<>();
            data.put("items",       cartService.getItems());
            data.put("totalAmount", cartService.getTotal());
            return ResponseEntity.ok(ApiResponse.success("Cập nhật số lượng thành công", data));
        } finally {
            CartServiceImpl.clearCartKey();
        }
    }

    @DeleteMapping("/remove/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> removeFromCart(
            @PathVariable("id") Integer id,
            @AuthenticationPrincipal String username,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            CartServiceImpl.setCartKey(resolveCartKey(username, request, response));
            cartService.remove(id);
            Map<String, Object> data = new HashMap<>();
            data.put("items",       cartService.getItems());
            data.put("totalAmount", cartService.getTotal());
            return ResponseEntity.ok(ApiResponse.success("Đã xóa sản phẩm khỏi giỏ hàng", data));
        } finally {
            CartServiceImpl.clearCartKey();
        }
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @AuthenticationPrincipal String username,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            CartServiceImpl.setCartKey(resolveCartKey(username, request, response));
            cartService.clear();
            return ResponseEntity.ok(ApiResponse.success("Đã xóa toàn bộ giỏ hàng", null));
        } finally {
            CartServiceImpl.clearCartKey();
        }
    }
}
