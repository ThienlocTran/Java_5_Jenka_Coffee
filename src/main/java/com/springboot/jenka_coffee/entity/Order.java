package com.springboot.jenka_coffee.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Orders") // Bắt buộc, vì Order trùng tên khóa SQL
public class Order implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;

    @Column(name = "Address", nullable = false)
    private String address;

    @Column(name = "CreateDate")
    private LocalDateTime createDate = LocalDateTime.now();

    @Column(name = "Phone", length = 15)
    private String phone;

    @Column(name = "Status")
    private Integer status = OrderStatus.NEW.getValue(); // 0: NEW, 1: CONFIRMED, 2: SHIPPING, 3: CANCELLED, 4: COMPLETED

    @Getter
    public enum OrderStatus {
        NEW(0), CONFIRMED(1), SHIPPING(2), CANCELLED(3), COMPLETED(4);

        private final int value;
        OrderStatus(int value) { this.value = value; }

        public static OrderStatus fromValue(int value) {
            for (OrderStatus s : values()) {
                if (s.value == value) return s;
            }
            throw new IllegalArgumentException("Unknown order status: " + value);
        }
    }

    @Column(name = "VoucherCode", length = 20)
    private String voucherCode;

    // 🚨 BUG-59: MISSING SHIPPING FEE ARCHITECTURE (Lỗ Sâu Vận Chuyển)
    // ================================================================
    // CRITICAL BUSINESS LOGIC GAP: No shipping fee field in database schema!
    // 
    // Current state:
    // - Frontend hardcodes "Phí vận chuyển: Miễn phí" (Free Shipping)
    // - Backend calculates: TotalAmount = Subtotal - Discount
    // - Order entity has NO shippingFee field
    // - CheckoutRequest has NO shippingFee field
    // - Database has NO shippingFee column
    // 
    // The problem:
    // System has NO WAY to charge shipping fees to customers!
    // 
    // Real-world scenario:
    // 1. Store wants to ship to remote provinces (shipping cost: 35,000đ)
    // 2. Customer orders 100,000đ worth of products
    // 3. System calculates total: 100,000đ (no shipping fee)
    // 4. Customer sees "Free Shipping" on website
    // 5. Customer pays 100,000đ
    // 6. Store must pay shipper 35,000đ out of pocket
    // 7. Store loses 35,000đ on every order
    // 
    // Business impact:
    // - Store loses money on every shipment
    // - Cannot charge different rates for different locations
    // - Cannot offer "Free shipping over 500,000đ" promotions
    // - Cannot integrate with shipping providers (Giao Hàng Nhanh, Giao Hàng Tiết Kiệm)
    // - Cannot sell this as SaaS e-commerce platform (missing critical feature)
    // 
    // Required changes (ARCHITECTURAL):
    // 
    // 1. Database migration:
    // ```sql
    // ALTER TABLE Orders ADD COLUMN shippingFee DECIMAL(18,2) DEFAULT 0;
    // ```
    // 
    // 2. Order entity (THIS FILE):
    // ```java
    // @Column(name = "shippingFee", precision = 18, scale = 2)
    // private BigDecimal shippingFee = BigDecimal.ZERO;
    // ```
    // 
    // 3. CheckoutRequest DTO:
    // Add field to receive shipping fee from frontend (or calculate server-side)
    // 
    // 4. OrderServiceImpl.checkout():
    // ```java
    // // Calculate shipping fee based on location
    // BigDecimal shippingFee = calculateShippingFee(request.getProvince(), request.getDistrict());
    // order.setShippingFee(shippingFee);
    // 
    // // Update total calculation
    // BigDecimal totalAmount = subtotal.subtract(discount).add(shippingFee);
    // order.setTotalAmount(totalAmount);
    // ```
    // 
    // 5. Shipping fee calculation service:
    // ```java
    // public BigDecimal calculateShippingFee(String province, String district) {
    //     // Option 1: Simple flat rate
    //     if ("Hà Nội".equals(province) || "TP. Hồ Chí Minh".equals(province)) {
    //         return new BigDecimal("30000"); // 30k for major cities
    //     } else {
    //         return new BigDecimal("50000"); // 50k for provinces
    //     }
    //     
    //     // Option 2: Integration with shipping provider API
    //     // return shippingProviderService.calculateFee(province, district, weight);
    //     
    //     // Option 3: Database-driven rates
    //     // return shippingRateRepository.findByLocation(province, district);
    // }
    // ```
    // 
    // 6. Free shipping promotion logic:
    // ```java
    // if (subtotal.compareTo(new BigDecimal("500000")) >= 0) {
    //     shippingFee = BigDecimal.ZERO; // Free shipping over 500k
    // }
    // ```
    // 
    // Frontend changes needed:
    // - CheckoutView.vue: Remove hardcoded "Miễn phí"
    // - Add API call to calculate shipping fee based on address
    // - Display calculated shipping fee in order summary
    // - Update total: Subtotal - Discount + Shipping
    // 
    // Technical considerations:
    // - Should shipping fee be calculated client-side or server-side? (Server-side recommended)
    // - Should shipping fee be editable by admin? (Yes, for manual adjustments)
    // - Should system integrate with real shipping providers? (Recommended for production)
    // - Should shipping fee be included in voucher discount calculation? (Usually no)
    // - Should free shipping be a voucher type? (Good idea for marketing)
    // 
    // Related entities:
    // - Order (this file) - needs shippingFee field
    // - CheckoutRequest - needs shippingFee field
    // - OrderServiceImpl - needs shipping calculation logic
    // - Frontend CheckoutView - needs UI updates
    // ================================================================

    @Column(name = "totalAmount", precision = 18, scale = 2)
    private BigDecimal totalAmount;

    // VULN-INFINITE-POINTS FIX: Lưu số điểm thực tế đã sử dụng khi checkout
    @Column(name = "pointsUsed")
    private Integer pointsUsed = 0;

    // VULN-058 FIX: note field — lưu ghi chú đơn hàng (đã sanitize trước khi save)
    @Column(name = "note", length = 500)
    private String note;

    // --- QUAN HỆ ---

    // N-1 với Account
    @JsonIgnore // Chặn Account↔Order cycle (tránh StackOverflow khi serialize)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Username")
    @ToString.Exclude
    private Account account;

    // 1-N với OrderDetail
    @JsonIgnore // Chặn Order↔OrderDetail cycle
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<OrderDetail> orderDetails;

    // N-1 với Voucher
    @JsonIgnoreProperties("orders") // Chặn Voucher↔Order cycle
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VoucherCode", insertable = false, updatable = false)
    @ToString.Exclude
    private Voucher voucher;

    // 1-N với Payment
    @JsonIgnore // Chặn Order↔Payment cycle
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Payment> payments;

    // 1-N với PointHistory
    @JsonIgnore // Chặn Order↔PointHistory cycle
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<PointHistory> pointHistories;

    // --- LOGIC HIBERNATE PROXY ---
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
        Order order = (Order) o;
        return getId() != null && Objects.equals(getId(), order.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }
}