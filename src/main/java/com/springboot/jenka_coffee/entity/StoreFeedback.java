package com.springboot.jenka_coffee.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "store_feedbacks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoreFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String branch; // "HN" or "HCM"

    @Column(nullable = false, length = 100)
    private String fullname;

    @Column(length = 20)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false)
    private Integer storeRating; // 1-5 stars

    @Column(nullable = false)
    private Integer staffRating; // 1-5 stars

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
