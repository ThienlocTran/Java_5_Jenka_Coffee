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

    /** 
     * Banner transition effect:
     * - fade: Mờ dần (default)
     * - slide: Trượt ngang
     * - zoom: Phóng to/thu nhỏ
     * - kenburns: Zoom + Pan (cinematic)
     * - push: Đẩy sang ngang
     * - curtain: Rèm kéo (wipe effect)
     * - parallax: Thị sai đa lớp (hiện đại)
     * - liquid: Vòng tròn mở rộng từ tâm
     * - wave: Sóng quét từ dưới lên
     * - magnetic: Trượt nghiêng đàn hồi
     * - blur: Lấy nét ống kính
     * - vortex: Xoáy vào từ tâm
     * - glitch: Nhiễu số hiện đại
     * - cube: Xoay 3D như khối lập phương
     * - flip: Lật 3D
     * - dissolve: Tan biến (blur + fade)
     * - scale-rotate: Phóng to + xoay
     * - prism: Lăng kính màu sắc
     * - none: Không có hiệu ứng
     */
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
