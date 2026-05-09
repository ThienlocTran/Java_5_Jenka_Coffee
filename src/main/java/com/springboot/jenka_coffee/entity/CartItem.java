package com.springboot.jenka_coffee.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Persistent Cart Item — replaces ConcurrentHashMap in CartServiceImpl.
 * cart_key = username for authenticated users.
 * cart_key = 'anon:<uuid>' for anonymous users (UUID stored in cookie).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "cart_items",
    uniqueConstraints = @UniqueConstraint(
        name = "cart_items_unique_item",
        columnNames = {"cart_key", "product_id"}
    )
)
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cart_key", nullable = false, length = 255)
    private String cartKey;

    @Column(name = "product_id", nullable = false)
    private Integer productId;

    @Column(nullable = false)
    private Integer quantity = 1;

    /** Giá tại thời điểm thêm vào giỏ (chỉ để hiển thị — khi checkout sẽ lấy giá từ DB) */
    @Column(name = "price_snapshot", nullable = false, precision = 18, scale = 2)
    private BigDecimal priceSnapshot;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "product_image", length = 500)
    private String productImage;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
