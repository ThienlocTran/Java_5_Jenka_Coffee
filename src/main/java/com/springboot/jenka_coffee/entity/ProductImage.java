package com.springboot.jenka_coffee.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ProductImages")
public class ProductImage implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Integer id;

    @Column(name = "ImageUrl", nullable = false)
    private String imageUrl;

    @Column(name = "DisplayOrder")
    private Integer displayOrder = 0;

    @Column(name = "IsPrimary")
    private Boolean isPrimary = false;

    @Column(name = "CreateDate", updatable = false)
    private LocalDateTime createDate = LocalDateTime.now();

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductId", nullable = false)
    @ToString.Exclude
    private Product product;
}
