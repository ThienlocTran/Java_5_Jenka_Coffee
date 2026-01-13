package com.springboot.jenka_coffee.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Entity
@Table(name = "Products")
public class Product implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;
    private String image;
    private Double price;

    @Temporal(TemporalType.DATE)
    @Column(name = "createDate")
    private Date createDate = new Date();

    private Boolean available;

    // Mapping quan hệ với Category (Many-to-One)
    // Cột trong DB tên là 'CategoryId', nên join column phải đặt đúng tên đó
    @ManyToOne
    @JoinColumn(name = "CategoryId")
    private Category category;

}