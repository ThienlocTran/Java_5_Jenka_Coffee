package com.springboot.jenka_coffee.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    /** FIXED = giảm cố định (VD: 50.000đ) | PERCENT = giảm % (VD: 10%) */
    @Column(name = "discountType", length = 10)
    private String discountType = "FIXED";

    @Column(name = "minOrderAmount", precision = 18, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(name = "expirationDate", nullable = false)
    private LocalDateTime expirationDate;

    /** Số lượt dùng còn lại. 0 = không giới hạn */
    @Column(name = "quantity")
    private Integer quantity = 0;

    @Column(name = "active")
    private Boolean active = true;

    /**
     * Phạm vi áp dụng:
     *   ALL      = áp dụng cho toàn bộ sản phẩm
     *   SPECIFIC = chỉ áp dụng cho các sản phẩm trong danh sách applicableProductIds
     */
    @Column(name = "scope", length = 10)
    private String scope = "ALL";

    /**
     * Danh sách product ID được áp dụng voucher (chỉ dùng khi scope = SPECIFIC).
     * Lưu dạng JSON array trong 1 column TEXT để tránh tạo bảng join thêm.
     * VD: "[1,2,5,10]"
     */
    @Column(name = "applicableProductIds", columnDefinition = "TEXT")
    private String applicableProductIds;

    // --- RELATIONSHIPS ---

    @JsonIgnore
    @OneToMany(mappedBy = "voucher", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Order> orders = new ArrayList<>();

    // --- BUSINESS LOGIC ---

    /** Kiểm tra voucher có áp dụng được cho product ID này không */
    public boolean isApplicableToProduct(Integer productId) {
        if ("ALL".equals(scope)) return true;
        if (applicableProductIds == null || applicableProductIds.isBlank()) return false;
        // Parse "[1,2,5]" → check contains
        String clean = applicableProductIds.replaceAll("[\\[\\]\\s]", "");
        for (String id : clean.split(",")) {
            if (id.equals(String.valueOf(productId))) return true;
        }
        return false;
    }

    /** Tính số tiền giảm thực tế dựa trên subtotal */
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        if ("PERCENT".equals(discountType)) {
            // discountAmount là % (VD: 10 = 10%)
            BigDecimal pct = discountAmount.min(BigDecimal.valueOf(100));
            return subtotal.multiply(pct).divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP);
        }
        // FIXED — không giảm quá subtotal
        return discountAmount.min(subtotal);
    }

    // --- HIBERNATE PROXY LOGIC ---
    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy
                ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
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
