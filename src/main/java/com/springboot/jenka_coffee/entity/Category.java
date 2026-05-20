package com.springboot.jenka_coffee.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Categories")
public class Category implements Serializable {

    @Id
    @Column(name = "Id", length = 50)
    private String id; // MÃ£ loáº¡i (VD: MAY_PHA) - Tá»± nháº­p, ko tá»± tÄƒng

    @Column(name = "Name", length = 100, nullable = false)
    private String name;

    @Column(name = "Icon")
    private String icon; // TÃªn file icon (VD: May_Pha_Ca_Phe.webp)

    @Column(name = "image_crop_x", precision = 5, scale = 2, nullable = false)
    private BigDecimal imageCropX = BigDecimal.ZERO;

    @Column(name = "image_crop_y", precision = 5, scale = 2, nullable = false)
    private BigDecimal imageCropY = BigDecimal.ZERO;

    @Column(name = "image_crop_width", precision = 5, scale = 2, nullable = false)
    private BigDecimal imageCropWidth = new BigDecimal("100.00");

    @Column(name = "image_crop_height", precision = 5, scale = 2, nullable = false)
    private BigDecimal imageCropHeight = new BigDecimal("100.00");

    @Column(name = "image_zoom", precision = 4, scale = 2, nullable = false)
    private BigDecimal imageZoom = new BigDecimal("1.00");

    // Quan há»‡ 1-N vá»›i Product
    @JsonIgnore // Cháº·n vÃ²ng láº·p vÃ´ táº­n JSON
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Product> products;

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
        Category category = (Category) o;
        return getId() != null && Objects.equals(getId(), category.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }
}
