package com.springboot.jenka_coffee.repository;

import com.springboot.jenka_coffee.dto.response.RevenueReportDTO;
import com.springboot.jenka_coffee.dto.response.TopCustomerDTO;
import com.springboot.jenka_coffee.entity.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // ===== ANALYTICS QUERIES =====

    /**
     * Get monthly revenue for a specific year
     * Returns revenue grouped by month
     */
    @Query("SELECT new com.springboot.jenka_coffee.dto.response.RevenueReportDTO(" +
            "YEAR(o.createDate), MONTH(o.createDate), SUM(o.totalAmount), COUNT(o)) " +
            "FROM Order o " +
            "WHERE YEAR(o.createDate) = :year " +
            "GROUP BY YEAR(o.createDate), MONTH(o.createDate) " +
            "ORDER BY MONTH(o.createDate)")
    List<RevenueReportDTO> getMonthlyRevenue(@Param("year") int year);

    /**
     * Get yearly revenue aggregation
     * Returns revenue grouped by year
     */
    @Query("SELECT new com.springboot.jenka_coffee.dto.response.RevenueReportDTO(" +
            "YEAR(o.createDate), NULL, SUM(o.totalAmount), COUNT(o)) " +
            "FROM Order o " +
            "GROUP BY YEAR(o.createDate) " +
            "ORDER BY YEAR(o.createDate) DESC")
    List<RevenueReportDTO> getYearlyRevenue();

    /**
     * Get top customers by total spending
     * Use Pageable to limit results
     */
    @Query("SELECT new com.springboot.jenka_coffee.dto.response.TopCustomerDTO(" +
            "a.username, a.fullname, SUM(o.totalAmount), COUNT(o)) " +
            "FROM Order o JOIN o.account a " +
            "GROUP BY a.username, a.fullname " +
            "ORDER BY SUM(o.totalAmount) DESC")
    List<TopCustomerDTO> getTopCustomers(Pageable pageable);
}
