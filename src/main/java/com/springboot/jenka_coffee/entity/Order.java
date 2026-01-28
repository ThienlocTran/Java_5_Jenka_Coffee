package com.springboot.jenka_coffee.entity;

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
    private Integer status = 0; // 0: NEW, 1: CONFIRMED, 2: SHIPPING, 3: CANCELLED, 4: COMPLETED

    @Column(name = "VoucherCode", length = 20)
    private String voucherCode;

    @Column(name = "totalAmount", precision = 18, scale = 2)
    private BigDecimal totalAmount;

    // --- QUAN HỆ ---

    // N-1 với Account
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Username")
    @ToString.Exclude
    private Account account;

    // 1-N với OrderDetail
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<OrderDetail> orderDetails;

    // N-1 với Voucher
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VoucherCode", insertable = false, updatable = false)
    @ToString.Exclude
    private Voucher voucher;

    // 1-N với Payment
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Payment> payments;

    // 1-N với PointHistory
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