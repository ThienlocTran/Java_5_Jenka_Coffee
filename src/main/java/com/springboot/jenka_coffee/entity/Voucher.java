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
@Table(name = "Vouchers")
public class Voucher implements Serializable {

    @Id
    @Column(name = "code", length = 20)
    private String code;

    @Column(name = "discountAmount", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "discountType", length = 10)
    private String discountType = "FIXED"; // FIXED or PERCENT

    @Column(name = "minOrderAmount", precision = 18, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(name = "expirationDate", nullable = false)
    private LocalDateTime expirationDate;

    @Column(name = "quantity")
    private Integer quantity = 0;

    @Column(name = "active")
    private Boolean active = true;

    // --- RELATIONSHIPS ---

    // 1-N with Order
    @OneToMany(mappedBy = "voucher", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Order> orders;

    // --- HIBERNATE PROXY LOGIC ---

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
        Voucher voucher = (Voucher) o;
        return getCode() != null && Objects.equals(getCode(), voucher.getCode());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }
}
