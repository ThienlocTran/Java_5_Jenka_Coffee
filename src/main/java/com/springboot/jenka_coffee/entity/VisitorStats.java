package com.springboot.jenka_coffee.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Persistent Visitor Stats — replaces AtomicLong in-memory counters.
 * One row per day. Updated via UPSERT on each visit.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "visitor_stats")
public class VisitorStats {

    @Id
    @Column(name = "stat_date")
    private LocalDate statDate;

    /** Number of unique visitors (by IP+UserAgent fingerprint) */
    @Column(name = "unique_visitors", nullable = false)
    private Integer uniqueVisitors = 0;

    /** Total visit count (same user can visit multiple times) */
    @Column(name = "total_visits", nullable = false)
    private Long totalVisits = 0L;

    /** Peak concurrent online count for the day */
    @Column(name = "online_peak", nullable = false)
    private Integer onlinePeak = 0;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
