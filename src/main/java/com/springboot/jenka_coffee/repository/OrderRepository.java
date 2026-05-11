package com.springboot.jenka_coffee.repository;

import com.springboot.jenka_coffee.dto.response.RevenueReportDTO;
import com.springboot.jenka_coffee.dto.response.TopCustomerDTO;
import com.springboot.jenka_coffee.dto.response.TopProductDTO;
import com.springboot.jenka_coffee.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // ===== PUBLIC ORDER CODE LOOKUPS =====

    /**
     * Find order by its public-facing order code (e.g. "ORD-20260511-AB12CD").
     * Used by customer-facing API instead of exposing numeric PK.
     */
    @Query("SELECT o FROM Order o " +
           "LEFT JOIN FETCH o.account " +
           "WHERE o.orderCode = :orderCode")
    java.util.Optional<Order> findByOrderCode(@Param("orderCode") String orderCode);

    /**
     * Find order by public code with all details eagerly loaded (avoids N+1).
     */
    @Query("SELECT o FROM Order o " +
           "LEFT JOIN FETCH o.account " +
           "LEFT JOIN FETCH o.orderDetails d " +
           "LEFT JOIN FETCH d.product " +
           "WHERE o.orderCode = :orderCode")
    java.util.Optional<Order> findByOrderCodeWithDetails(@Param("orderCode") String orderCode);

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



    Page<Order> findByAccount_Username(String username, Pageable pageable);

    /**
     * Fetch orders with orderDetails and products for order history page
     * Uses JOIN FETCH to eagerly load relationships and avoid N+1 queries
     */
    @Query(value = "SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.orderDetails d " +
            "LEFT JOIN FETCH d.product " +
            "WHERE o.account.username = :username",
            countQuery = "SELECT COUNT(o) FROM Order o WHERE o.account.username = :username")
    Page<Order> findByUsernameWithDetails(@Param("username") String username, Pageable pageable);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.account WHERE o.id IN :ids")
    List<Order> findAllWithAccountByIds(@Param("ids") List<Long> ids);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.account LEFT JOIN FETCH o.orderDetails d LEFT JOIN FETCH d.product WHERE o.id = :id")
    java.util.Optional<Order> findByIdWithDetails(@Param("id") Long id);

    // ===== AGGREGATE STATS =====

    @Query("SELECT COUNT(o) FROM Order o")
    long countAllOrders();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    long countByStatus(@Param("status") int status);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o")
    BigDecimal sumTotalRevenue();

    long countByCreateDateGreaterThanEqualAndCreateDateLessThan(LocalDateTime from, LocalDateTime to);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.createDate >= :from AND o.createDate < :to")
    BigDecimal sumTotalRevenueBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // ===== TOP PRODUCTS =====
    @Query("SELECT new com.springboot.jenka_coffee.dto.response.TopProductDTO(" +
            "d.product.id, d.product.name, d.product.category.name, SUM(d.quantity), SUM(d.price * d.quantity)) " +
            "FROM OrderDetail d " +
            "GROUP BY d.product.id, d.product.name, d.product.category.name " +
            "ORDER BY SUM(d.quantity) DESC")
    List<TopProductDTO> getTopProducts(Pageable pageable);

    // ===== REVENUE BY PERIOD =====
    @Query("SELECT new com.springboot.jenka_coffee.dto.response.RevenueReportDTO(" +
            "EXTRACT(YEAR FROM o.createDate), EXTRACT(MONTH FROM o.createDate), SUM(o.totalAmount), COUNT(o)) " +
            "FROM Order o " +
            "WHERE o.createDate >= :from AND o.createDate <= :to " +
            "GROUP BY EXTRACT(YEAR FROM o.createDate), EXTRACT(MONTH FROM o.createDate) " +
            "ORDER BY EXTRACT(YEAR FROM o.createDate), EXTRACT(MONTH FROM o.createDate)")
    List<RevenueReportDTO> getRevenueByDateRange(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

}
