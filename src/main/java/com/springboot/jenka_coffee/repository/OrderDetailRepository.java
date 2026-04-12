package com.springboot.jenka_coffee.repository;

import com.springboot.jenka_coffee.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
    
    /**
     * VULN-CONSTRAINT-VIOLATION FIX: Check if product is used in any orders
     * Used to prevent deletion of products that have order history
     */
    @Query("SELECT COUNT(od) FROM OrderDetail od WHERE od.product.id = :productId")
    long countByProductId(@Param("productId") Integer productId);
    
    /**
     * Check if product exists in any orders
     */
    boolean existsByProduct_Id(Integer productId);
}
