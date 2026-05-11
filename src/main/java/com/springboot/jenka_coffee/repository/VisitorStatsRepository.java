package com.springboot.jenka_coffee.repository;

import com.springboot.jenka_coffee.entity.VisitorStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface VisitorStatsRepository extends JpaRepository<VisitorStats, LocalDate> {

    Optional<VisitorStats> findByStatDate(LocalDate date);

    @Query(value = "SELECT COALESCE(SUM(total_visits), 0) FROM visitor_stats", nativeQuery = true)
    long sumTotalVisits();

    @Query(value = "SELECT COALESCE(SUM(total_visits), 0) FROM visitor_stats WHERE stat_date >= :fromDate", nativeQuery = true)
    long sumTotalVisitsSince(@Param("fromDate") LocalDate fromDate);

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO visitor_stats (stat_date, unique_visitors, total_visits, online_peak, updated_at)
        VALUES (:today, :uniqueDelta, 1, :onlinePeak, NOW())
        ON CONFLICT (stat_date) DO UPDATE SET
            total_visits    = visitor_stats.total_visits + 1,
            unique_visitors = visitor_stats.unique_visitors + :uniqueDelta,
            online_peak     = GREATEST(visitor_stats.online_peak, :onlinePeak),
            updated_at      = NOW()
        """, nativeQuery = true)
    void upsertVisit(
            @Param("today") LocalDate today,
            @Param("uniqueDelta") int uniqueDelta,
            @Param("onlinePeak") int onlinePeak
    );
}
