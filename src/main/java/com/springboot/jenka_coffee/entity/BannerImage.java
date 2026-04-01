package com.springboot.jenka_coffee.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "banner_image")
public class BannerImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banner_set_id", nullable = false)
    private BannerSet bannerSet;

    @Column(nullable = false, length = 500)
    private String image;

    @Column(length = 200)
    private String title;

    @Column(length = 300)
    private String subtitle;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}
