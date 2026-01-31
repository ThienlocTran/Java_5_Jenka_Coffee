package com.springboot.jenka_coffee.repository;

import com.springboot.jenka_coffee.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    // ===== EXISTING METHODS =====

    /**
     * Find products by category
     */
    List<Product> findByCategoryId(String cid);

    /**
     * Find related products (same category, exclude current product)
     */
    List<Product> findTop4ByCategoryIdAndIdNot(String categoryId, Integer id);

    /**
     * Count products by category (for delete validation)
     */
    long countByCategoryId(String categoryId);

    // ===== NEW METHODS =====

    /**
     * Find products by category with pagination
     */
    Page<Product> findByCategoryId(String categoryId, Pageable pageable);

    /**
     * Find available products only (available = true)
     * DSL method - Spring generates: SELECT * FROM products WHERE available = true
     */
    List<Product> findByAvailableTrue();

    /**
     * Search products by name or description
     * JPQL required: multiple fields with LIKE + case-insensitive
     */
    @Query("SELECT p FROM Product p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Product> searchProducts(@Param("keyword") String keyword);

    /**
     * Find products by price range
     * DSL method - Spring generates: SELECT * FROM products WHERE price BETWEEN ?
     * AND ?
     */
    List<Product> findByPriceBetween(Double minPrice, Double maxPrice);
}