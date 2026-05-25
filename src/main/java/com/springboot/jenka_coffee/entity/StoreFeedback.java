package com.springboot.jenka_coffee.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "store_feedbacks")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
public class StoreFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "branch", length = 100)
    private String branch;

    @Column(name = "fullname", nullable = false, length = 100)
    private String fullname;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private FeedbackStatus status;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "store_rating")
    private Integer storeRating;

    @Column(name = "staff_rating")
    private Integer staffRating;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = FeedbackStatus.PENDING;
        }
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        StoreFeedback that = (StoreFeedback) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
