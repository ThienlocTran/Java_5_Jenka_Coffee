package com.springboot.jenka_coffee.repository;

import com.springboot.jenka_coffee.entity.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointHistoryDAO extends JpaRepository<PointHistory, Long> {

    // Tìm lịch sử điểm theo username, sắp xếp theo ngày tạo giảm dần
    List<PointHistory> findByUsernameOrderByCreateDateDesc(String username);

    // Tìm lịch sử điểm theo OrderId
    List<PointHistory> findByOrderId(Long orderId);

    // Tính tổng điểm của user
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PointHistory p WHERE p.username = ?1")
    Integer getTotalPointsByUsername(String username);
}
