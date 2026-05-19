package com.springboot.jenka_coffee.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serializable;
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
    private String id; // MÃ£ loáº¡i (VD: MAY_PHA) - Tá»± nháº­p, ko tá»± tÄƒng

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "icon")
    private String icon; // TÃªn file icon (VD: May_Pha_Ca_Phe.webp)

    @Column(name = "slug", length = 160)
    private String slug;

    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "meta_title", length = 255)
    private String metaTitle;

    @Column(name = "meta_description", length = 320)
    private String metaDescription;

    @Lob
    @Column(name = "seo_content", columnDefinition = "TEXT")
    private String seoContent;

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
