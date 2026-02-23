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

    // Paginated version
    Page<Product> findByCategoryId(String categoryId, Pageable pageable);

    List<Product> findTop4ByCategoryIdAndIdNot(String categoryId, Integer id);

    /**
     * Count products by category (for delete validation)
     */
    long countByCategoryId(String categoryId);

    // ===== NEW METHODS =====

    /**
     * Find products by category with pagination
     */


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

    // ========== ADVANCED FILTER METHODS WITH PAGINATION ==========

    /**
     * Find products by category and price range with pagination
     */
    @Query("SELECT p FROM Product p WHERE " +
            "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
            "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Product> findByCategoryAndPriceRange(@Param("categoryId") String categoryId,
                                              @Param("minPrice") Double minPrice,
                                              @Param("maxPrice") Double maxPrice,
                                              Pageable pageable);

    /**
     * Search products by keyword with pagination
     */
    @Query("SELECT p FROM Product p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Product> searchProductsPaginated(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Filter products by all criteria with pagination
     */
    @Query("SELECT p FROM Product p WHERE " +
            "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
            "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
            "(:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> findByAllCriteria(@Param("categoryId") String categoryId,
                                   @Param("minPrice") Double minPrice,
                                   @Param("maxPrice") Double maxPrice,
                                   @Param("keyword") String keyword,
                                   Pageable pageable);
}