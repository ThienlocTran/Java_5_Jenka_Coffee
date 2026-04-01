package com.springboot.jenka_coffee.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "banner_set")
public class BannerSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /** fade | slide | zoom | kenburns */
    @Column(nullable = false, length = 30)
    private String effect = "fade";

    /** Chỉ 1 bộ được active tại 1 thời điểm */
    @Column(nullable = false)
    private Boolean active = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @JsonManagedReference
    @OneToMany(mappedBy = "bannerSet", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("sortOrder ASC")
    private List<BannerImage> images = new ArrayList<>();
}
