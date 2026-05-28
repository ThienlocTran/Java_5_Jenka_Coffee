package com.springboot.jenka_coffee.api;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.service.CartService;
import com.springboot.jenka_coffee.service.impl.CartServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping("/api/cart")
public class ApiCartController {

    private static final String ANONYMOUS_CART_HEADER = "X-Anonymous-Cart-Id";
    private static final Pattern SAFE_ANONYMOUS_CART_ID = Pattern.compile("^[A-Za-z0-9_-]{16,80}$");

    private final CartService cartService;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    public ApiCartController(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * Resolve cart key:
     * - Authenticated: username (persistent forever in DB)
     * - Anonymous: 'anon:<uuid>' from X-Anonymous-Cart-Id header
     *
     * Business rules:
     * - Logged-in user cart NEVER expires → encourages purchase across sessions/devices
     * - Anonymous cart is scoped to the browser/device generated id
     */
    private String resolveCartKey(String username, HttpServletRequest request, HttpServletResponse response) {
        return resolveCartKey(username, request, response, false);
    }

    private String resolveCartKey(String username, HttpServletRequest request, HttpServletResponse response, boolean requireAnonymousCartId) {
        if (isAuthenticatedUsername(username)) return username;

        String anonymousCartId = request.getHeader(ANONYMOUS_CART_HEADER);
        if (isSafeAnonymousCartId(anonymousCartId)) {
            return "anon:" + anonymousCartId;
        }

        if (requireAnonymousCartId) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing X-Anonymous-Cart-Id");
        }

        // Frontend v2 always sends X-Anonymous-Cart-Id. If an old client misses it,
        // use a request-scoped fallback instead of trusting legacy cart_id cookies.
        String cartUuid = UUID.randomUUID().toString();
        log.debug("[CART] Missing anonymous cart header, using request fallback: anon:{}", cartUuid);
        return "anon:" + cartUuid;
    }

    private boolean isSafeAnonymousCartId(String value) {
        return value != null && SAFE_ANONYMOUS_CART_ID.matcher(value).matches();
    }

    private boolean isAuthenticatedUsername(String username) {
        return username != null && !username.isBlank() && !"anonymousUser".equals(username);
    }

    private void logCartRequest(String endpoint, String username, HttpServletRequest request, String cartKey, int itemCount) {
        if (!isLocalProfile()) {
            return;
        }
        log.info("[CART DEBUG] endpoint={}, principal={}, anonymousCartId={}, resolvedCartKey={}, itemCount={}",
                endpoint, username, request.getHeader(ANONYMOUS_CART_HEADER), cartKey, itemCount);
    }

    private boolean isLocalProfile() {
        return "local".equals(activeProfile) || "default".equals(activeProfile) || activeProfile.contains("dev");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCart(
            @AuthenticationPrincipal String username,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            String cartKey = resolveCartKey(username, request, response);
            CartServiceImpl.setCartKey(cartKey);
            int itemCount = cartService.getCount();
            logCartRequest("GET /api/cart", username, request, cartKey, itemCount);
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
            String cartKey = resolveCartKey(username, request, response, true);
            CartServiceImpl.setCartKey(cartKey);
            logCartRequest("POST /api/cart/add/" + id, username, request, cartKey, cartService.getCount());
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
            String cartKey = resolveCartKey(username, request, response, true);
            CartServiceImpl.setCartKey(cartKey);
            cartService.update(id, qty);
            logCartRequest("PUT /api/cart/update/" + id, username, request, cartKey, cartService.getCount());
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
            String cartKey = resolveCartKey(username, request, response, true);
            CartServiceImpl.setCartKey(cartKey);
            cartService.remove(id);
            logCartRequest("DELETE /api/cart/remove/" + id, username, request, cartKey, cartService.getCount());
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
            String cartKey = resolveCartKey(username, request, response, true);
            CartServiceImpl.setCartKey(cartKey);
            cartService.clear();
            logCartRequest("DELETE /api/cart/clear", username, request, cartKey, 0);
            return ResponseEntity.ok(ApiResponse.success("Đã xóa toàn bộ giỏ hàng", null));
        } finally {
            CartServiceImpl.clearCartKey();
        }
    }
}
