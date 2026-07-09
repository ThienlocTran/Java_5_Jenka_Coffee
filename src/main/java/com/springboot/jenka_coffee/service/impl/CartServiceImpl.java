package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.dto.response.CartItem;
import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.exception.BusinessRuleException;
import com.springboot.jenka_coffee.repository.CartItemRepository;
import com.springboot.jenka_coffee.service.CartService;
import com.springboot.jenka_coffee.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * DB-backed CartService — replaces in-memory ConcurrentHashMap.
 * Cart data is persisted in cart_items table → survives server restart.
 *
 * cart_key for authenticated users: username
 * cart_key for anonymous users: 'anon:<uuid>' (UUID from X-Anonymous-Cart-Id header)
 */
@Slf4j
@Service
public class CartServiceImpl implements CartService {

    private final ObjectProvider<ProductService> productServiceProvider;
    private final CartItemRepository cartItemRepository;

    // ThreadLocal to pass request context (anonymous user UUID)
    private static final ThreadLocal<String> cartKeyContext = new ThreadLocal<>();

    public CartServiceImpl(ObjectProvider<ProductService> productServiceProvider,
                           CartItemRepository cartItemRepository) {
        this.productServiceProvider = productServiceProvider;
        this.cartItemRepository = cartItemRepository;
    }

    private ProductService productService() {
        return productServiceProvider.getObject();
    }

    /**
     * Set cart key for current request.
     * Called by ApiCartController before each operation.
     * - Authenticated: username
     * - Anonymous: 'anon:<uuid>' from X-Anonymous-Cart-Id header
     */
    public static void setCartKey(String cartKey) {
        cartKeyContext.set(cartKey);
    }

    public static void clearCartKey() {
        cartKeyContext.remove();
    }

    private String cartKey() {
        String key = cartKeyContext.get();
        if (key != null) return key;

        // Fallback: try to get from SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }

        throw new BusinessRuleException("Không thể xác định giỏ hàng. Vui lòng thử lại!");
    }

    // =========================================================
    // Converter: entity CartItem → DTO CartItem
    // =========================================================
    private CartItem toDto(com.springboot.jenka_coffee.entity.CartItem entity) {
        CartItem dto = new CartItem();
        dto.setProductId(entity.getProductId());
        dto.setName(entity.getProductName());
        dto.setImage(entity.getProductImage());
        dto.setPrice(entity.getPriceSnapshot());
        dto.setQuantity(entity.getQuantity());
        return dto;
    }

    // =========================================================
    // CART OPERATIONS
    // =========================================================

    @Override
    @Transactional
    public void add(Integer productId) {
        String key = cartKey();

        Optional<com.springboot.jenka_coffee.entity.CartItem> existing =
                cartItemRepository.findByCartKeyAndProductId(key, productId);

        if (existing.isPresent()) {
            com.springboot.jenka_coffee.entity.CartItem item = existing.get();
            if (item.getQuantity() >= 99) {
                throw new BusinessRuleException("Số lượng tối đa mỗi sản phẩm là 99!");
            }
            item.setQuantity(item.getQuantity() + 1);
            cartItemRepository.save(item);
        } else {
            Product product = productService().findById(productId);

            if (product == null || !Boolean.TRUE.equals(product.getAvailable())) {
                throw new BusinessRuleException("Sản phẩm không tồn tại hoặc không còn kinh doanh!");
            }
            if (Boolean.TRUE.equals(product.getRequireContact())) {
                throw new BusinessRuleException("Sản phẩm này cần liên hệ để được tư vấn mua hàng.");
            }
            if (product.getPrice() == null || product.getPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new BusinessRuleException("Sản phẩm này cần liên hệ để được tư vấn mua hàng.");
            }

            com.springboot.jenka_coffee.entity.CartItem item = new com.springboot.jenka_coffee.entity.CartItem();
            item.setCartKey(key);
            item.setProductId(product.getId());
            item.setQuantity(1);
            item.setPriceSnapshot(product.getPrice());
            item.setProductName(product.getName());
            item.setProductImage(product.getImage());
            cartItemRepository.save(item);
        }
    }

    @Override
    @Transactional
    public void remove(Integer productId) {
        cartItemRepository.deleteByCartKeyAndProductId(cartKey(), productId);
    }

    @Override
    @Transactional
    public void update(Integer productId, int qty) {
        if (qty <= 0) {
            cartItemRepository.deleteByCartKeyAndProductId(cartKey(), productId);
            return;
        }
        if (qty > 99) throw new BusinessRuleException("Số lượng tối đa mỗi sản phẩm là 99!");

        cartItemRepository.findByCartKeyAndProductId(cartKey(), productId).ifPresent(item -> {
            item.setQuantity(qty);
            cartItemRepository.save(item);
        });
    }

    @Override
    @Transactional
    public void clear() {
        cartItemRepository.deleteByCartKey(cartKey());
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CartItem> getItems() {
        return cartItemRepository.findByCartKey(cartKey())
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public int getCount() {
        return (int) cartItemRepository.countByCartKey(cartKey());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotal() {
        return cartItemRepository.findByCartKey(cartKey()).stream()
                .map(i -> i.getPriceSnapshot().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getCartSummary() {
        java.util.Map<String, Object> res = new java.util.HashMap<>();
        List<com.springboot.jenka_coffee.entity.CartItem> items =
                cartItemRepository.findByCartKey(cartKey());
        BigDecimal total = items.stream()
                .map(i -> i.getPriceSnapshot().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        res.put("count", items.size());
        res.put("total", total);
        res.put("items", items.stream().map(this::toDto).collect(Collectors.toList()));
        return res;
    }

    @Override
    @Transactional
    public void mergeAnonymousCart(String username, String anonymousCartId) {
        if (username == null || username.isBlank() || anonymousCartId == null || anonymousCartId.isBlank()) {
            return;
        }

        String anonymousKey = "anon:" + anonymousCartId;
        List<com.springboot.jenka_coffee.entity.CartItem> anonymousItems =
                cartItemRepository.findByCartKey(anonymousKey);
        if (anonymousItems.isEmpty()) {
            return;
        }

        for (com.springboot.jenka_coffee.entity.CartItem anonymousItem : anonymousItems) {
            Optional<com.springboot.jenka_coffee.entity.CartItem> existingUserItem =
                    cartItemRepository.findByCartKeyAndProductId(username, anonymousItem.getProductId());

            if (existingUserItem.isPresent()) {
                com.springboot.jenka_coffee.entity.CartItem userItem = existingUserItem.get();
                int mergedQuantity = Math.min(99, userItem.getQuantity() + anonymousItem.getQuantity());
                userItem.setQuantity(mergedQuantity);
                cartItemRepository.save(userItem);
            } else {
                anonymousItem.setCartKey(username);
                cartItemRepository.save(anonymousItem);
            }
        }

        cartItemRepository.deleteByCartKey(anonymousKey);
    }

    /**
     * Cleanup: xóa anonymous cart items (anon:*) không được cập nhật trong 2 giờ.
     * (Anonymous carts là session-only — cookie bị mất khi đóng browser,
     *  nhưng DB row vẫn còn → cần cleanup định kỳ)
     *
     * Logged-in user carts KHÔNG bị xóa — tồn tại vĩnh viễn cho đến khi
     * user checkout hoặc tự clear. Mục đích: kích thích quay lại mua hàng.
     *
     * Chạy mỗi 1 giờ.
     */
    @Scheduled(fixedDelay = 3_600_000)
    @Transactional
    public void evictStaleCartItems() {
        // Anonymous carts: cleanup sau 2 giờ không hoạt động
        LocalDateTime threshold = LocalDateTime.now().minusHours(2);
        int deleted = cartItemRepository.deleteStaleAnonymousCartItems(threshold);
        if (deleted > 0) {
            log.info("[CART CLEANUP] Deleted {} stale anonymous cart items (older than 2h)", deleted);
        }
    }
}
