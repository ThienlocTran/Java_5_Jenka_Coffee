package com.springboot.jenka_coffee.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Entity
@Table(name = "Bookings")
@Data
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerName;
    private String phone;
    private String email;
    private String machineModel; // Loại máy cần sửa
    private String description;  // Tình trạng hư hỏng

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime bookingDate; // Ngày giờ hẹn

    private Integer status = 0; // 0: Pending (Chờ), 1: Done (Đã sửa)
    private LocalDateTime createDate = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "username")
    private Account account; // Link tới tài khoản nếu khách đã đăng nhập
}