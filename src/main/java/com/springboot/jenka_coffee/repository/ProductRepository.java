package com.springboot.jenka_coffee.repository;

import com.springboot.jenka_coffee.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    // ── findAll with JOIN FETCH to avoid LazyInitializationException ──
    @Query(value = "SELECT p FROM Product p JOIN FETCH p.category",
           countQuery = "SELECT COUNT(p) FROM Product p")
    Page<Product> findAllWithCategory(Pageable pageable);
    
    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.id = :id")
    Optional<Product> findByIdWithCategory(@Param("id") Integer id);
    
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.images LEFT JOIN FETCH p.category WHERE p.id = :id")
    Optional<Product> findByIdWithImages(@Param("id") Integer id);

    // ── By category ──────────────────────────────────────────────────
    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.category.id = :cid")
    List<Product> findByCategoryId(@Param("cid") String cid);

    @Query(value = "SELECT p FROM Product p JOIN FETCH p.category WHERE p.category.id = :categoryId",
           countQuery = "SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId")
    Page<Product> findByCategoryId(@Param("categoryId") String categoryId, Pageable pageable);

    long countByCategoryId(String categoryId);

    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.category.id = :categoryId AND p.id <> :id AND p.available = true")
    List<Product> findTop4ByCategoryIdAndIdNot(@Param("categoryId") String categoryId, @Param("id") Integer id,
                                               Pageable pageable);

    // ── Category counts ──────────────────────────────────────────────
    @Query("SELECT p.category.id, COUNT(p) FROM Product p WHERE p.available = true GROUP BY p.category.id")
    List<Object[]> countProductsGroupedByCategory();

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("UPDATE Product p SET p.quantity = :quantity WHERE p.id = :id")
    int updateQuantityById(@Param("id") Integer id, @Param("quantity") Integer quantity);

    // ── Available only ───────────────────────────────────────────────
    // (findByAvailableTrue removed - unused, use findByAllCriteria instead)

    // ── Search (paginated only) ──────────────────────────────────────
    @Query(value = "SELECT p FROM Product p JOIN FETCH p.category WHERE " +
                   "(:categoryId IS NULL OR :categoryId = '' OR p.category.id = :categoryId) AND " +
                   "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
                   "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
                   "p.available = true AND " +
                   "(:keyword IS NULL OR :keyword = '' OR " +
                   " LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                   " LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))",
           countQuery = "SELECT COUNT(p) FROM Product p WHERE " +
                        "(:categoryId IS NULL OR :categoryId = '' OR p.category.id = :categoryId) AND " +
                        "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
                        "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
                        "p.available = true AND " +
                        "(:keyword IS NULL OR :keyword = '' OR " +
                        " LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        " LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> findByAllCriteria(@Param("categoryId") String categoryId,
                                    @Param("minPrice") BigDecimal minPrice,
                                    @Param("maxPrice") BigDecimal maxPrice,
                                    @Param("keyword") String keyword,
                                    Pageable pageable);

    @Query(value = "SELECT p FROM Product p JOIN FETCH p.category WHERE " +
                   "p.available = true AND " +
                   "(:keyword IS NULL OR :keyword = '' OR " +
                   " LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                   " LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))",
           countQuery = "SELECT COUNT(p) FROM Product p WHERE " +
                        "p.available = true AND " +
                        "(:keyword IS NULL OR :keyword = '' OR " +
                        " LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        " LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchProductsPaginated(@Param("keyword") String keyword, Pageable pageable);
}
