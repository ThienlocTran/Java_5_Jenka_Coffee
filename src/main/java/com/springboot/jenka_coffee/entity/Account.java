package com.springboot.jenka_coffee.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;

import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

@SuppressWarnings("serial")
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Entity
@Table(name = "Accounts")
public class Account implements Serializable {
    @Id
    String username;
    String password;
    String fullname;
    String email;
    String photo;
    Boolean activated;
    Boolean admin;

    // Quan hệ: Một User có nhiều Đơn hàng
    @JsonIgnore // Chặn vòng lặp vô tận khi chuyển sang JSON
    @ToString.Exclude // Chặn Lombok in ra list này gây lỗi tràn bộ nhớ
    @OneToMany(mappedBy = "account")
    List<Order> orders;
}