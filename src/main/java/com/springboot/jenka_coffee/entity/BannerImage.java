package com.springboot.jenka_coffee.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "banner_image")
public class BannerImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banner_set_id", nullable = false)
    private BannerSet bannerSet;

    @Column(name = "image", nullable = false, length = 500)
    private String image;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "subtitle", length = 300)
    private String subtitle;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "image_crop_x", nullable = false, precision = 5, scale = 2)
    private BigDecimal imageCropX = BigDecimal.ZERO;

    @Column(name = "image_crop_y", nullable = false, precision = 5, scale = 2)
    private BigDecimal imageCropY = BigDecimal.ZERO;

    @Column(name = "image_crop_width", nullable = false, precision = 5, scale = 2)
    private BigDecimal imageCropWidth = new BigDecimal("100.00");

    @Column(name = "image_crop_height", nullable = false, precision = 5, scale = 2)
    private BigDecimal imageCropHeight = new BigDecimal("100.00");

    @Column(name = "image_zoom", nullable = false, precision = 4, scale = 2)
    private BigDecimal imageZoom = new BigDecimal("1.00");
}
