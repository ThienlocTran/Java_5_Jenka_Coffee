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

    // VULN-OOM-001 FIX: Static ObjectMapper để tránh tạo mới mỗi lần gọi
    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = 
            new com.fasterxml.jackson.databind.ObjectMapper();

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
     *   CATEGORY = chỉ áp dụng cho các sản phẩm thuộc category trong applicableCategoryIds
     */
    @Column(name = "scope", length = 10)
    private String scope = "ALL";
    
    /**
     * BUG-44 FIX: Maximum number of times a user can use this voucher
     * null or 0 = unlimited uses per user
     * 1 = one-time use per user (default behavior)
     * N = user can use this voucher N times
     * 
     * Example: FREESHIP voucher with maxUsesPerUser=30 allows daily use during promotion month
     */
    @Column(name = "maxUsesPerUser")
    private Integer maxUsesPerUser = 1;

    /**
     * Danh sách product ID được áp dụng voucher (chỉ dùng khi scope = SPECIFIC).
     * Lưu dạng JSON array trong 1 column TEXT để tránh tạo bảng join thêm.
     * VD: "[1,2,5,10]"
     */
    @Column(name = "applicableProductIds", columnDefinition = "TEXT")
    private String applicableProductIds;

    /**
     * Danh sách category ID được áp dụng voucher (chỉ dùng khi scope = CATEGORY).
     * Lưu dạng JSON array trong 1 column TEXT.
     * VD: "[1,3,5]"
     */
    @Column(name = "applicableCategoryIds", columnDefinition = "TEXT")
    private String applicableCategoryIds;

    // --- RELATIONSHIPS ---

    @JsonIgnore
    @OneToMany(mappedBy = "voucher", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Order> orders = new ArrayList<>();

    // --- BUSINESS LOGIC ---

    /** Kiểm tra voucher có áp dụng được cho product này không */
    public boolean isApplicableToProduct(Integer productId, Integer categoryId) {
        if ("ALL".equals(scope)) return true;
        
        if ("SPECIFIC".equals(scope)) {
            return checkProductInList(productId, applicableProductIds);
        }
        
        if ("CATEGORY".equals(scope)) {
            return checkCategoryInList(categoryId, applicableCategoryIds);
        }
        
        return false;
    }
    
    /** Backward compatibility - kiểm tra chỉ theo product ID */
    public boolean isApplicableToProduct(Integer productId) {
        if ("ALL".equals(scope)) return true;
        if ("SPECIFIC".equals(scope)) {
            return checkProductInList(productId, applicableProductIds);
        }
        return false;
    }
    
    private boolean checkProductInList(Integer productId, String idListJson) {
        if (idListJson == null || idListJson.isBlank()) return false;
        // VULN-021 & VULN-OOM-001 FIX: Dùng static ObjectMapper
        try {
            java.util.List<Integer> ids = MAPPER.readValue(idListJson,
                    MAPPER.getTypeFactory().constructCollectionType(java.util.List.class, Integer.class));
            return ids.contains(productId);
        } catch (Exception e) {
            // Fallback: parse thủ công nếu JSON malformed
            String clean = idListJson.replaceAll("[\\[\\]\\s]", "");
            if (clean.isBlank()) return false;
            for (String id : clean.split(",")) {
                try {
                    if (Integer.parseInt(id.trim()) == productId) return true;
                } catch (NumberFormatException ignored) { /* skip malformed entries */ }
            }
            return false;
        }
    }
    
    private boolean checkCategoryInList(Integer categoryId, String idListJson) {
        if (categoryId == null) return false;
        return checkProductInList(categoryId, idListJson); // Same logic
    }

    /** 
     * Tính số tiền giảm thực tế dựa trên subtotal 
     * VULN-UNCAPPED-VOUCHER FIX: Add maxDiscountAmount cap for percentage vouchers
     */
    @Column(name = "maxDiscountAmount", precision = 18, scale = 2)
    private BigDecimal maxDiscountAmount;
    
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        if ("PERCENT".equals(discountType)) {
            // discountAmount là % (VD: 10 = 10%)
            BigDecimal pct = discountAmount.min(BigDecimal.valueOf(100));
            BigDecimal discount = subtotal.multiply(pct).divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP);
            
            // VULN-UNCAPPED-VOUCHER FIX: Cap discount at maxDiscountAmount if set
            if (maxDiscountAmount != null && maxDiscountAmount.compareTo(BigDecimal.ZERO) > 0) {
                discount = discount.min(maxDiscountAmount);
            }
            
            return discount;
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
