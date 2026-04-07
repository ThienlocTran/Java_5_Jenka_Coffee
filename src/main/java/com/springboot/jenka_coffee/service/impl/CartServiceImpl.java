package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.dto.response.CartItem;
import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.service.CartService;
import com.springboot.jenka_coffee.service.ProductService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cart lưu in-memory per username — tương thích với JWT stateless.
 * Thay thế @SessionScope (không hoạt động với stateless JWT).
 *
 * Trade-off: cart mất khi server restart. Acceptable cho quy mô hiện tại.
 * Production scale: chuyển sang Redis hoặc bảng CartItems trong DB.
 */
@Service
public class CartServiceImpl implements CartService {

    private final ObjectProvider<ProductService> productServiceProvider;

    // Map<username, Map<productId, CartItem>>
    private final ConcurrentHashMap<String, Map<Integer, CartItem>> userCarts = new ConcurrentHashMap<>();

    public CartServiceImpl(ObjectProvider<ProductService> productServiceProvider) {
        this.productServiceProvider = productServiceProvider;
    }

    private ProductService productService() {
        return productServiceProvider.getObject();
    }

    /** Lấy username từ JWT SecurityContext */
    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "anonymous";
        }
        return auth.getName();
    }

    /** Lấy cart của user hiện tại, tạo mới nếu chưa có */
    private Map<Integer, CartItem> cart() {
        return userCarts.computeIfAbsent(currentUser(), k -> new HashMap<>());
    }

    @Override
    public void add(Integer productId) {
        Map<Integer, CartItem> map = cart();
        CartItem item = map.get(productId);
        if (item == null) {
            Product product = productService().findById(productId);
            if (product != null) {
                item = new CartItem();
                item.setProductId(product.getId());
                item.setName(product.getName());
                item.setImage(product.getImage());
                item.setPrice(product.getPrice());
                item.setQuantity(1);
                map.put(productId, item);
            }
        } else {
            item.setQuantity(item.getQuantity() + 1);
        }
    }

    @Override
    public void remove(Integer productId) {
        cart().remove(productId);
    }

    @Override
    public void update(Integer productId, int qty) {
        if (qty <= 0) { cart().remove(productId); return; }
        CartItem item = cart().get(productId);
        if (item != null) item.setQuantity(qty);
    }

    @Override
    public void clear() {
        userCarts.remove(currentUser());
    }

    @Override
    public Collection<CartItem> getItems() {
        return cart().values();
    }

    @Override
    public int getCount() {
        return cart().size();
    }

    @Override
    public BigDecimal getTotal() {
        return cart().values().stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public Map<String, Object> getCartSummary() {
        Map<String, Object> res = new HashMap<>();
        res.put("count", getCount());
        res.put("total", getTotal());
        res.put("items", getItems());
        return res;
    }

    /**
     * Dọn dẹp cart của user không hoạt động — tránh RAM phình to.
     * Chạy mỗi 2 giờ, xóa cart của anonymous và cart rỗng.
     */
    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 7_200_000)
    public void evictStaleCarts() {
        userCarts.entrySet().removeIf(e ->
            "anonymous".equals(e.getKey()) || e.getValue().isEmpty()
        );
    }
}
