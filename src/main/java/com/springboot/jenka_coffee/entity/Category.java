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

    /**
     * Production VPS categories schema does not guarantee these optional crop columns.
     * Keep them out of Hibernate mapping so public category/product reads do not fail.
     */
    @Transient
    private BigDecimal imageCropX = BigDecimal.ZERO;

    @Transient
    private BigDecimal imageCropY = BigDecimal.ZERO;

    @Transient
    private BigDecimal imageCropWidth = new BigDecimal("100.00");

    @Transient
    private BigDecimal imageCropHeight = new BigDecimal("100.00");

    @Transient
    private BigDecimal imageZoom = new BigDecimal("1.00");

    @JsonIgnore
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Product> products;

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
