package com.springboot.jenka_coffee.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.persistence.Index;
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
@Table(
    name = "Orders", // Bắt buộc, vì Order trùng tên khóa SQL
    indexes = { @Index(name = "idx_order_code", columnList = "orderCode", unique = true) }
)
public class Order implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;

    /**
     * Public-facing order code — safe to expose in URLs and to customers.
     * Format: ORD-YYYYMMDD-XXXXXX  (e.g. ORD-20260511-AB12CD)
     * Generated once at checkout; NEVER changes after creation.
     * Keeps numeric PK private (performance / JPA relations unchanged).
     */
    @Column(name = "orderCode", unique = true, nullable = false, length = 30)
    private String orderCode;

    @Column(name = "Address", nullable = false)
    private String address;

    @Column(name = "CreateDate")
    private LocalDateTime createDate = LocalDateTime.now();

    @Column(name = "Phone", length = 15)
    private String phone;

    @Column(name = "Status")
    private Integer status = OrderStatus.NEW.getValue(); // 0: NEW, 1: CONFIRMED, 2: CANCELLED

    /**
     * Simplified Order Status - No shipping tracking
     * Business model: Admin confirms order, then calls customer to arrange delivery
     */
    @Getter
    public enum OrderStatus {
        NEW(0),        // Đơn hàng mới, chờ xác nhận
        CONFIRMED(1),  // Đã xác nhận, admin sẽ gọi khách và giao hàng
        CANCELLED(2);  // Đã hủy

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