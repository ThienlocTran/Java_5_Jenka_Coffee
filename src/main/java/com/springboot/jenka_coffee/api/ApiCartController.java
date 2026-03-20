package com.springboot.jenka_coffee.api;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class ApiCartController {

    private final CartService cartService;

    public ApiCartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCart() {
        Map<String, Object> data = new HashMap<>();
        data.put("items", cartService.getItems());
        data.put("totalAmount", cartService.getTotal());
        data.put("summary", cartService.getCartSummary());
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin giỏ hàng thành công", data));
    }

    @PostMapping("/add/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addToCart(@PathVariable("id") Integer id) {
        cartService.add(id);

        Map<String, Object> data = new HashMap<>();
        data.put("summary", cartService.getCartSummary());
        return ResponseEntity.ok(ApiResponse.success("Đã thêm sản phẩm vào giỏ hàng", data));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateCartItem(
            @PathVariable("id") Integer id,
            @RequestParam("qty") int qty) {

        cartService.update(id, qty);

        Map<String, Object> data = new HashMap<>();
        data.put("items", cartService.getItems());
        data.put("totalAmount", cartService.getTotal());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật số lượng thành công", data));
    }

    @DeleteMapping("/remove/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> removeFromCart(@PathVariable("id") Integer id) {
        cartService.remove(id);

        Map<String, Object> data = new HashMap<>();
        data.put("items", cartService.getItems());
        data.put("totalAmount", cartService.getTotal());
        return ResponseEntity.ok(ApiResponse.success("Tính lại giỏ hàng thành công", data));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<Void>> clearCart() {
        cartService.clear();
        return ResponseEntity.ok(ApiResponse.success("Đã xóa toàn bộ giỏ hàng", null));
    }
}
