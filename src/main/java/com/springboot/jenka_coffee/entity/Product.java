package com.springboot.jenka_coffee.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * BUG-53 WARNING: Missing Core Inventory Management
 * 
 * PROBLEM: No stock quantity tracking for physical products
 * - System designed for coffee shop (unlimited drinks)
 * - But also sells expensive machines (limited stock)
 * - No stockQuantity field in Product entity
 * - No inventory deduction during checkout
 * 
 * BUSINESS FAILURE SCENARIO:
 * Store has 1 Espresso machine in stock (50 million VND)
 * Two customers simultaneously:
 * 1. Customer A adds machine to cart, clicks checkout
 * 2. Customer B adds same machine to cart, clicks checkout
 * 3. Both orders succeed (no stock check)
 * 4. Both receive "Order Confirmed" email
 * 5. Next day: Admin realizes only 1 machine in stock
 * 6. Must call one customer: "Sorry, out of stock, refund"
 * 7. Customer angry, leaves bad review
 * 8. Lost sale + damaged reputation
 * 
 * CURRENT IMPLEMENTATION:
 * - Product entity has: id, name, price, description, available (boolean)
 * - No stockQuantity field
 * - No inventory tracking
 * - No stock deduction during checkout
 * - No low stock alerts
 * - No overselling prevention
 * 
 * MISSING FEATURES:
 * 1. Stock quantity tracking
 * 2. Inventory deduction on order
 * 3. Inventory restoration on cancellation
 * 4. Low stock warnings
 * 5. Out of stock prevention
 * 6. Reserved stock during checkout
 * 7. Inventory history/audit trail
 * 8. Multi-warehouse support
 * 
 * PRODUCTION SOLUTION:
 * 
 * 1. ADD INVENTORY FIELDS TO PRODUCT:
 *    - manageInventory (boolean): Enable/disable stock tracking per product
 *    - stockQuantity (integer): Current available stock
 *    - reservedQuantity (integer): Stock reserved in pending orders
 *    - lowStockThreshold (integer): Alert when stock below this
 *    - allowBackorder (boolean): Allow orders when out of stock
 *    
 * 2. CREATE INVENTORY TRANSACTION TABLE:
 *    - Track all stock changes (orders, returns, adjustments)
 *    - Audit trail for compliance
 *    - Fields: productId, type, quantity, orderId, date, note
 *    
 * 3. UPDATE CHECKOUT FLOW:
 *    a. Lock product with PESSIMISTIC_WRITE
 *    b. Check if manageInventory = true
 *    c. Verify stockQuantity >= orderQuantity
 *    d. Deduct from stockQuantity
 *    e. Create inventory transaction record
 *    f. If stock < lowStockThreshold: send alert
 *    
 * 4. HANDLE ORDER CANCELLATION:
 *    - Restore stockQuantity when order cancelled
 *    - Create reversal inventory transaction
 *    - Update reserved quantity
 *    
 * 5. ADMIN FEATURES:
 *    - Inventory management dashboard
 *    - Stock adjustment interface
 *    - Low stock alerts
 *    - Inventory reports
 *    - Stock movement history
 * 
 * MIGRATION STEPS:
 * 1. Add inventory fields to Product entity
 * 2. Create InventoryTransaction entity
 * 3. Create database migration script
 * 4. Set manageInventory=false for drinks (unlimited)
 * 5. Set manageInventory=true for machines (limited)
 * 6. Update checkout service to check/deduct stock
 * 7. Update order cancellation to restore stock
 * 8. Add admin inventory management UI
 * 9. Implement low stock alerts
 * 10. Test with concurrent orders
 * 
 * EXAMPLE MIGRATION SQL:
 * ```sql
 * ALTER TABLE Products ADD COLUMN manageInventory BIT DEFAULT 0;
 * ALTER TABLE Products ADD COLUMN stockQuantity INT DEFAULT 0;
 * ALTER TABLE Products ADD COLUMN reservedQuantity INT DEFAULT 0;
 * ALTER TABLE Products ADD COLUMN lowStockThreshold INT DEFAULT 5;
 * ALTER TABLE Products ADD COLUMN allowBackorder BIT DEFAULT 0;
 * 
 * CREATE TABLE InventoryTransactions (
 *   id BIGINT PRIMARY KEY AUTO_INCREMENT,
 *   productId INT NOT NULL,
 *   transactionType VARCHAR(50) NOT NULL, -- ORDER, CANCEL, ADJUSTMENT, RETURN
 *   quantity INT NOT NULL,
 *   orderId BIGINT,
 *   note NVARCHAR(500),
 *   createdDate DATETIME DEFAULT CURRENT_TIMESTAMP,
 *   createdBy VARCHAR(50),
 *   FOREIGN KEY (productId) REFERENCES Products(Id)
 * );
 * ```
 * 
 * RISK LEVEL: Critical for e-commerce with physical products
 * BUSINESS IMPACT: Very High (overselling, customer complaints, refunds)
 * EFFORT: High (1-2 weeks for full implementation)
 * PRIORITY: High (should implement before scaling)
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Products")
public class Product implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Integer id;

    @Column(name = "Name", length = 200, nullable = false)
    private String name;

    @Column(name = "slug", length = 255, unique = true)
    private String slug;

    @Column(name = "Image")
    private String image;

    @Min(value = 0, message = "Giá sản phẩm phải lớn hơn 0")
    @Column(name = "price", nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "createDate", updatable = false)
    private LocalDateTime createDate = LocalDateTime.now();

    @Column(name = "Available")
    private Boolean available = true;

    // Featured product flag for homepage highlighting
    @Column(name = "isFeatured")
    private Boolean featured = false;

    @Column(name = "requireContact")
    private Boolean requireContact = false; // Sản phẩm yêu cầu liên hệ (không thể mua online)

    // --- CÁC MỐI QUAN HỆ ---

    @JsonIgnoreProperties({"products", "hibernateLazyInitializer", "handler"}) // Chặn Category↔Product cycle và lỗi proxy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Categoryid", nullable = false)
    @ToString.Exclude
    private Category category;

    @JsonIgnore // Chặn Product↔OrderDetail cycle
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<OrderDetail> orderDetails;

    @JsonIgnore // Không serialize images trong Product response - dùng API riêng để lấy
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    @ToString.Exclude
    private List<ProductImage> images;

    // Đoạn code tránh Lazy của Hibernate nè

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null)
            return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy
                ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();
        if (thisEffectiveClass != oEffectiveClass)
            return false;
        Product product = (Product) o;
        return getId() != null && Objects.equals(getId(), product.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }
}