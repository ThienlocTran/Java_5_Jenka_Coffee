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

    @Column(name = "Password", nullable = false)
    private String password;

    @Column(name = "Fullname", nullable = false) // Thêm nullable=false cho chặt chẽ
    private String fullname;

    @Column(name = "Email", nullable = false)
    private String email;

    @Column(name = "Photo")
    private String photo;

    @Column(name = "Activated")
    private Boolean activated = true;

    @Column(name = "Admin")
    private Boolean admin = false;

    // Quan hệ 1-N với Order
    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY)
    // Tôi bỏ cascade=ALL ở đây nhé, vì xóa User không nên xóa luôn lịch sử đơn hàng (nghiệp vụ thực tế)
    // Nếu bài Lab yêu cầu xóa hết thì thêm cascade = CascadeType.ALL vào lại.
    @ToString.Exclude
    private List<Order> orders;

    // --- LOGIC HIBERNATE PROXY (Chuẩn chỉ) ---

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Account account = (Account) o;
        // So sánh Username (String) thay vì ID (Integer/Long)
        return getUsername() != null && Objects.equals(getUsername(), account.getUsername());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}