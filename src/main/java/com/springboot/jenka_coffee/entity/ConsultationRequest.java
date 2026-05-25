package com.springboot.jenka_coffee.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "consultation_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "contact_phone", nullable = false, length = 50)
    private String contactPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "need_type", nullable = false, length = 20)
    private ConsultationNeedType needType;

    @Enumerated(EnumType.STRING)
    @Column(name = "interest", nullable = false, length = 30)
    private ConsultationInterest interest;

    @Enumerated(EnumType.STRING)
    @Column(name = "budget", nullable = false, length = 20)
    private ConsultationBudget budget;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "source", length = 50)
    private String source;

    @Column(name = "product_name", length = 200)
    private String productName;

    @Column(name = "product_url", length = 500)
    private String productUrl;

    @Column(name = "page_title", length = 200)
    private String pageTitle;

    @Column(name = "page_url", length = 500)
    private String pageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ConsultationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (status == null) {
            status = ConsultationStatus.NEW;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
