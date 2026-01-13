package com.springboot.jenka_coffee.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;

import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@SuppressWarnings("serial")
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Entity
@Table(name = "Orders") // Bắt buộc vì Order là từ khóa trong SQL
public class Order implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String address;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "Createdate")
    Date createDate = new Date();

    String phone;

    Integer status; // 0: Mới, 1: Duyệt...

    // Quan hệ: Nhiều Đơn hàng thuộc 1 User
    @ManyToOne
    @JoinColumn(name = "Username")
    Account account;

    // Quan hệ: Một Đơn hàng có nhiều Chi tiết
    @ToString.Exclude
    @OneToMany(mappedBy = "order")
    List<OrderDetail> orderDetails;
}