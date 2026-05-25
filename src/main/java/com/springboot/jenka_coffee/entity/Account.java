package com.springboot.jenka_coffee.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.domain.Persistable;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "accounts")
public class Account implements Serializable, Persistable<String> {

    @Id
    @Column(name = "username", length = 50)
    private String username;

    /**
     * JPA FIX: String @Id causes merge() instead of persist()
     * Implement Persistable to explicitly control isNew() behavior
     * This flag is set by service layer when creating new accounts
     */
    @Transient
    private boolean isNew = false;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "fullname", nullable = false)
    private String fullname;

    @Column(name = "email", length = 100, unique = true)
    private String email;

    /**
     * Convert empty email to NULL to avoid unique constraint violations.
     */
    @PrePersist
    @PreUpdate
    private void normalizeEmail() {
        if (email != null) {
            email = email.trim().toLowerCase();
            if (email.isEmpty()) {
                email = null;
            }
        }
    }

    @Column(name = "phone", length = 15, unique = true)
    private String phone;

    @Column(name = "phone_verified")
    private Boolean phoneVerified = false;

    @Column(name = "photo")
    private String photo;

    @Column(name = "activated")
    private Boolean activated = true;

    @Column(name = "admin")
    private Boolean admin = false;

    @Column(name = "points")
    private Integer points = 0;

    @Column(name = "customer_rank", length = 20)
    private String customerRank = "MEMBER";

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "activation_token", length = 100)
    private String activationToken;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "activation_token_expiry")
    private LocalDateTime activationTokenExpiry;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "reset_token", length = 100)
    private String resetToken;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "reset_token_expiry")
    private LocalDateTime resetTokenExpiry;

    @Column(name = "activation_method", length = 10)
    private String activationMethod;

    /**
     * Used to invalidate old JWT tokens after password reset.
     */
    @Column(name = "last_password_reset_date")
    private LocalDateTime lastPasswordResetDate;

    @Column(name = "create_date", nullable = false, updatable = false)
    private LocalDateTime createDate = LocalDateTime.now();

    @JsonIgnore
    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Order> orders;

    @JsonIgnore
    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<PointHistory> pointHistories;

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
        Account account = (Account) o;
        return getUsername() != null && Objects.equals(getUsername(), account.getUsername());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }

    @Override
    public String getId() {
        return username;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    /**
     * Reset isNew flag after persist/load to prevent re-insertion.
     */
    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }
}
