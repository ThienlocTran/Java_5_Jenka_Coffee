package com.springboot.jenka_coffee.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

@SuppressWarnings("serial")
@Data
@Entity
@Table(name = "Categories")
public class Category implements Serializable {
    @Id
    String id;
    String name;

    // Quan hệ: Một Loại có nhiều Sản phẩm
    @JsonIgnore
    @ToString.Exclude
    @OneToMany(mappedBy = "category")
    List<Product> products;
}