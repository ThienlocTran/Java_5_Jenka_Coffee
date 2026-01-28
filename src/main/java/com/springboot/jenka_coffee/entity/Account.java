package com.springboot.jenka_coffee.entity;

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
@Table(name = "Accounts")
public class Account implements Serializable {

    @Id
    @Column(name = "Username", length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "Fullname", nullable = false) // Thêm nullable=false cho chặt chẽ
    private String fullname;

    @Column(name = "Email", nullable = false, length = 100, unique = true)
    private String email;

    @Column(name = "phone", length = 15, unique = true)
    private String phone;

    @Column(name = "phone_verified")
    private Boolean phoneVerified = false;

    @Column(name = "Photo", length = 255)
    private String photo;

    @Column(name = "Activated")
    private Boolean activated = true;

    @Column(name = "Admin")
    private Boolean admin = false;

    @Column(name = "points")
    private Integer points = 0;

    @Column(name = "customer_rank", length = 20)
    private String customerRank = "MEMBER"; // MEMBER, SILVER, GOLD, DIAMOND

    // Quan hệ 1-N với Order
    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Order> orders;

    // Quan hệ 1-N với PointHistory
    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<PointHistory> pointHistories;

    // Quan hệ 1-N với ServiceBooking
    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<ServiceBooking> serviceBookings;

    // --- LOGIC HIBERNATE PROXY (Chuẩn chỉ) ---

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
        Account account = (Account) o;
        // So sánh Username (String) thay vì ID (Integer/Long)
        return getUsername() != null && Objects.equals(getUsername(), account.getUsername());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }
}