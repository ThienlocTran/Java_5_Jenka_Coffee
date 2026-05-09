package com.springboot.jenka_coffee.repository;

import com.springboot.jenka_coffee.entity.VisitorStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VisitorStatsRepository extends JpaRepository<VisitorStats, LocalDate> {

    Optional<VisitorStats> findByStatDate(LocalDate date);

    /** Lấy thống kê 30 ngày gần nhất */
    @Query("SELECT v FROM VisitorStats v WHERE v.statDate >= :fromDate ORDER BY v.statDate DESC")
    List<VisitorStats> findRecentStats(@Param("fromDate") LocalDate fromDate);

    /** UPSERT: tăng total_visits và cập nhật unique_visitors theo ngày */
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
            @Param("uniqueDelta") int uniqueDelta,   // 1 nếu visitor mới, 0 nếu đã đếm
            @Param("onlinePeak") int onlinePeak
    );
}
