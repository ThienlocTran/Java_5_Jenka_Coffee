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
 * BUG-51 WARNING: Stateful In-Memory Cart Kills Horizontal Scaling
 * 
 * PROBLEM: Cart stored in ConcurrentHashMap in application memory
 * - Backend uses JWT (stateless) but cart is stateful (in-memory)
 * - Works fine on single server but breaks with load balancing
 * 
 * SCALING FAILURE SCENARIO:
 * Customer deploys 3 servers (A, B, C) behind load balancer (Nginx/AWS ELB)
 * 1. User visits site → Load balancer routes to Server A
 * 2. User adds coffee to cart → Stored in Server A's RAM
 * 3. User refreshes page → Load balancer routes to Server B
 * 4. Server B checks its RAM → Empty! Cart disappeared!
 * 5. User goes to checkout → Cart is empty, customer angry
 * 
 * CURRENT IMPLEMENTATION:
 * - ConcurrentHashMap<String, CartEntry> userCarts (in-memory only)
 * - Cart data lives only in single JVM instance
 * - Lost on server restart or load balancer routing
 * - TTL eviction after 1 hour of inactivity
 * - Max 5000 carts to prevent memory exhaustion
 * 
 * PRODUCTION SOLUTIONS:
 * 
 * 1. REDIS CACHE (Recommended for high traffic):
 *    - Store cart in Redis with TTL
 *    - All servers share same Redis instance
 *    - Fast read/write (sub-millisecond)
 *    - Automatic expiration
 *    - Example: Spring Data Redis + @Cacheable
 *    
 * 2. DATABASE PERSISTENCE (Recommended for reliability):
 *    - Create Cart and CartItem tables
 *    - Store cart in database per user
 *    - Survives server restarts
 *    - Can implement abandoned cart recovery
 *    - Slower than Redis but more reliable
 *    
 * 3. HYBRID APPROACH (Best of both worlds):
 *    - Write-through cache: Redis + Database
 *    - Redis for fast reads
 *    - Database for persistence
 *    - Background sync job
 *    
 * 4. STICKY SESSIONS (Not recommended):
 *    - Load balancer routes same user to same server
 *    - Defeats purpose of load balancing
 *    - Server failure = lost carts
 *    - Don't use this approach
 * 
 * MIGRATION STEPS:
 * 1. Create Cart/CartItem entities and repositories
 * 2. Implement database-backed CartService
 * 3. Add Redis caching layer (optional)
 * 4. Migrate existing in-memory carts (if any)
 * 5. Update frontend to handle cart sync
 * 
 * RISK LEVEL: Critical for production scaling
 * BUSINESS IMPACT: High (lost sales, customer frustration)
 * EFFORT: Medium (2-3 days for database, 1 day for Redis)
 * 
 * Cart lưu in-memory per username — tương thích với JWT stateless.
 * Thay thế @SessionScope (không hoạt động với stateless JWT).
 * Trade-off: cart mất khi server restart. Acceptable cho quy mô hiện tại.
 * Production scale: chuyển sang Redis hoặc bảng CartItems trong DB.
 */
@Service
public class CartServiceImpl implements CartService {

    private final ObjectProvider<ProductService> productServiceProvider;

    // Map<username, CartEntry> — CartEntry có timestamp để TTL eviction
    private final ConcurrentHashMap<String, CartEntry> userCarts = new ConcurrentHashMap<>();

    private static final int MAX_CARTS = 5000;          // giới hạn tổng số cart
    private static final long CART_TTL_MS = 3_600_000L; // 1 giờ không hoạt động → xóa

    private static class CartEntry {
        // VULN-DDOS-001 FIX: Dùng ConcurrentHashMap thay vì HashMap để tránh race condition
        final Map<Integer, CartItem> items = new java.util.concurrent.ConcurrentHashMap<>();
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
        if (qty > 99) throw new com.springboot.jenka_coffee.exception.BusinessRuleException(
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
    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 1_800_000)
    public void evictStaleCarts() {
        userCarts.entrySet().removeIf(e -> e.getValue().isExpired() || e.getValue().items.isEmpty());
    }
}
