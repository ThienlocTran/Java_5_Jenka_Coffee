package com.springboot.jenka_coffee.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;

@SuppressWarnings("serial")
@Data
@Entity
@Table(name = "Orderdetails")
public class OrderDetail implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    Double price;
    Integer quantity;

    // Quan hệ: Thuộc về 1 Đơn hàng
    @ManyToOne
    @JoinColumn(name = "Orderid")
    Order order;

    // Quan hệ: Là 1 Sản phẩm
    @ManyToOne
    @JoinColumn(name = "Productid")
    Product product;
}