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
@Table(name = "categories")
public class Category implements Serializable {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "icon", length = 500)
    private String icon;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "meta_title", length = 255)
    private String metaTitle;

    @Column(name = "meta_description", length = 500)
    private String metaDescription;

    @Column(name = "seo_content", columnDefinition = "TEXT")
    private String seoContent;

    @Column(name = "slug", length = 300, unique = true)
    private String slug;

    @Column(name = "image_crop_x", nullable = false, precision = 5, scale = 2)
    private BigDecimal imageCropX = BigDecimal.ZERO;

    @Column(name = "image_crop_y", nullable = false, precision = 5, scale = 2)
    private BigDecimal imageCropY = BigDecimal.ZERO;

    @Column(name = "image_crop_width", nullable = false, precision = 5, scale = 2)
    private BigDecimal imageCropWidth = new BigDecimal("100.00");

    @Column(name = "image_crop_height", nullable = false, precision = 5, scale = 2)
    private BigDecimal imageCropHeight = new BigDecimal("100.00");

    @Column(name = "image_zoom", nullable = false, precision = 5, scale = 2)
    private BigDecimal imageZoom = new BigDecimal("1.00");

    @JsonIgnore
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Product> products;

    @Transient
    private Long productCount;

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        Class<?> oEffectiveClass = o instanceof HibernateProxy
                ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) {
            return false;
        }
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
