package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.dto.response.CartItem;
import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.exception.BusinessRuleException;
import com.springboot.jenka_coffee.service.CartService;
import com.springboot.jenka_coffee.service.ProductService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// BUG-51 WARNING: Stateful In-Memory Cart Kills Horizontal Scaling
@Service
public class CartServiceImpl implements CartService {

    private final ObjectProvider<ProductService> productServiceProvider;

    // Map<username, CartEntry> — CartEntry có timestamp để TTL eviction
    private final ConcurrentHashMap<String, CartEntry> userCarts = new ConcurrentHashMap<>();

    private static final int MAX_CARTS = 5000;          // giới hạn tổng số cart
    private static final long CART_TTL_MS = 3_600_000L; // 1 giờ không hoạt động → xóa

    private static class CartEntry {
        // VULN-DDOS-001 FIX: Dùng ConcurrentHashMap thay vì HashMap để tránh race condition
        final Map<Integer, CartItem> items = new ConcurrentHashMap<>();
        volatile long lastAccess = System.currentTimeMillis();

        void touch() { lastAccess = System.currentTimeMillis(); }
        boolean isExpired() { return System.currentTimeMillis() - lastAccess > CART_TTL_MS; }
    }

    public CartServiceImpl(ObjectProvider<ProductService> productServiceProvider) {
        this.productServiceProvider = productServiceProvider;
    }

    private ProductService productService() {
        return productServiceProvider.getObject();
    }

    /** VULN-024 FIX: Không cho anonymous dùng cart — yêu cầu đăng nhập.
     *  Trước đây tất cả anonymous dùng chung key "anonymous" → cart poisoning. */
    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new com.springboot.jenka_coffee.exception.BusinessRuleException(
                    "Vui lòng đăng nhập để sử dụng giỏ hàng!");
        }
        return auth.getName();
    }

    /** Lấy cart của user hiện tại, tạo mới nếu chưa có */
    private Map<Integer, CartItem> cart() {
        // VULN-M06 FIX: Giới hạn tổng số cart để tránh memory exhaustion
        String user = currentUser();
        CartEntry entry = userCarts.computeIfAbsent(user, k -> {
            if (userCarts.size() >= MAX_CARTS) {
                // Xóa 1 entry cũ nhất khi đầy
                userCarts.entrySet().stream()
                        .min(java.util.Comparator.comparingLong(e -> e.getValue().lastAccess))
                        .map(java.util.Map.Entry::getKey)
                        .ifPresent(userCarts::remove);
            }
            return new CartEntry();
        });
        entry.touch(); // cập nhật lastAccess
        return entry.items;
    }

    @Override
    public void add(Integer productId) {
        Map<Integer, CartItem> map = cart();
        CartItem item = map.get(productId);
        if (item == null) {
            Product product = productService().findById(productId);
            if (product != null) {
                // VULN-PRODUCT-AVAILABILITY FIX: Check if product is available
                if (!Boolean.TRUE.equals(product.getAvailable())) {
                    throw new com.springboot.jenka_coffee.exception.BusinessRuleException(
                            "Sản phẩm này hiện không còn kinh doanh!");
                }
                
                // SECURITY: Prevent adding requireContact products to cart
                if (Boolean.TRUE.equals(product.getRequireContact())) {
                    throw new com.springboot.jenka_coffee.exception.BusinessRuleException(
                            "Sản phẩm này yêu cầu liên hệ trực tiếp, không thể thêm vào giỏ hàng");
                }
                
                item = new CartItem();
                item.setProductId(product.getId());
                item.setName(product.getName());
                item.setImage(product.getImage());
                item.setPrice(product.getPrice());
                item.setQuantity(1);
                map.put(productId, item);
            }
        } else {
            // FVULN-003 FIX: Giới hạn max 99 per item
            if (item.getQuantity() >= 99) {
                throw new com.springboot.jenka_coffee.exception.BusinessRuleException(
                        "Số lượng tối đa mỗi sản phẩm là 99!");
            }
            item.setQuantity(item.getQuantity() + 1);
        }
    }

    @Override
    public void remove(Integer productId) {
        cart().remove(productId);
    }

    @Override
    public void update(Integer productId, int qty) {
        // FVULN-003 FIX: Validate qty — ngăn bypass qua API trực tiếp
        if (qty <= 0) { cart().remove(productId); return; }
        if (qty > 99) throw new BusinessRuleException(
                "Số lượng tối đa mỗi sản phẩm là 99!");
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
     * VULN-M06 FIX: Evict theo TTL thực sự — không chỉ xóa cart rỗng.
     * Chạy mỗi 30 phút, xóa cart không hoạt động quá 1 giờ.
     */
    @Scheduled(fixedDelay = 1_800_000)
    public void evictStaleCarts() {
        userCarts.entrySet().removeIf(e -> e.getValue().isExpired() || e.getValue().items.isEmpty());
    }
}
