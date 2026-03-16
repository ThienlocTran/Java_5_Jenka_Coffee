package com.springboot.jenka_coffee.repository;

import com.springboot.jenka_coffee.dto.response.RevenueReportDTO;
import com.springboot.jenka_coffee.dto.response.TopCustomerDTO;
import com.springboot.jenka_coffee.entity.Order;
import org.springframework.data.domain.Page;
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
            "EXTRACT(YEAR FROM o.createDate), EXTRACT(MONTH FROM o.createDate), SUM(o.totalAmount), COUNT(o)) " +
            "FROM Order o " +
            "WHERE EXTRACT(YEAR FROM o.createDate) = :year " +
            "GROUP BY EXTRACT(YEAR FROM o.createDate), EXTRACT(MONTH FROM o.createDate) " +
            "ORDER BY EXTRACT(MONTH FROM o.createDate)")
    List<RevenueReportDTO> getMonthlyRevenue(@Param("year") int year);

    @Query("SELECT new com.springboot.jenka_coffee.dto.response.RevenueReportDTO(" +
            "EXTRACT(YEAR FROM o.createDate), null, SUM(o.totalAmount), COUNT(o)) " +
            "FROM Order o " +
            "GROUP BY EXTRACT(YEAR FROM o.createDate) " +
            "ORDER BY EXTRACT(YEAR FROM o.createDate) DESC")
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


    List<Order> findByAccount_Username(String username);
    Page<Order> findByAccount_Username(String username, Pageable pageable);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.account WHERE o.id IN :ids")
    List<Order> findAllWithAccountByIds(@Param("ids") List<Long> ids);
}
