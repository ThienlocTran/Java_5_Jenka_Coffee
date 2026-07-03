package com.springboot.jenka_coffee.repository;

import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.entity.ProductKind;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    // ── Tìm theo slug ────────────────────────────────────────────────
    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.slug = :slug")
    Optional<Product> findBySlugWithCategory(@Param("slug") String slug);
    
    boolean existsBySlug(String slug);

    // ── findAll with JOIN FETCH to avoid LazyInitializationException ──
    // Featured products first by explicit homepage position, then by creation date.
    @Query(value = "SELECT p FROM Product p JOIN FETCH p.category " +
            "ORDER BY CASE WHEN p.featured = true THEN 0 ELSE 1 END, " +
            "CASE WHEN p.featured = true THEN COALESCE(p.featuredPosition, 999999) ELSE 999999 END ASC, " +
            "p.createDate DESC",
           countQuery = "SELECT COUNT(p) FROM Product p")
    Page<Product> findAllWithCategory(Pageable pageable);

    @Query(value = "SELECT p FROM Product p JOIN FETCH p.category WHERE p.available = true ORDER BY p.createDate DESC",
           countQuery = "SELECT COUNT(p) FROM Product p WHERE p.available = true")
    Page<Product> findByAvailableTrueWithCategory(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.featured = true ORDER BY COALESCE(p.featuredPosition, 999999), p.createDate DESC")
    List<Product> findFeaturedProductsForOrdering();

    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.available = true ORDER BY p.createDate DESC")
    List<Product> findAvailableForHomepageOrdering();
    
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

    // ── Available only ───────────────────────────────────────────────
    // (findByAvailableTrue removed - unused, use findByAllCriteria instead)

    // ── Search (paginated only) ──────────────────────────────────────
    @Query(value = "SELECT p FROM Product p JOIN FETCH p.category WHERE " +
                   "(:categoryId IS NULL OR :categoryId = '' OR p.category.id = :categoryId) AND " +
                   "(:categorySlug IS NULL OR :categorySlug = '' OR LOWER(p.category.slug) = LOWER(:categorySlug)) AND " +
                   "(:productKind IS NULL OR p.productKind = :productKind) AND " +
                   "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
                   "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
                   "p.available = true AND " +
                   "(:keyword IS NULL OR :keyword = '' OR " +
                   " LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                   " LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))",
           countQuery = "SELECT COUNT(p) FROM Product p WHERE " +
                        "(:categoryId IS NULL OR :categoryId = '' OR p.category.id = :categoryId) AND " +
                        "(:categorySlug IS NULL OR :categorySlug = '' OR LOWER(p.category.slug) = LOWER(:categorySlug)) AND " +
                        "(:productKind IS NULL OR p.productKind = :productKind) AND " +
                        "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
                        "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
                        "p.available = true AND " +
                        "(:keyword IS NULL OR :keyword = '' OR " +
                        " LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        " LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> findByAllCriteria(@Param("categoryId") String categoryId,
                                    @Param("categorySlug") String categorySlug,
                                    @Param("productKind") ProductKind productKind,
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
    
    // ── Admin filtered list (keyword + categoryId + available) ───────
    @Query(value = "SELECT p FROM Product p JOIN FETCH p.category WHERE " +
                   "(:categoryId IS NULL OR :categoryId = '' OR p.category.id = :categoryId) AND " +
                   "(:available IS NULL OR p.available = :available) AND " +
                   "(:keyword IS NULL OR :keyword = '' OR " +
                   " LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))",
           countQuery = "SELECT COUNT(p) FROM Product p WHERE " +
                        "(:categoryId IS NULL OR :categoryId = '' OR p.category.id = :categoryId) AND " +
                        "(:available IS NULL OR p.available = :available) AND " +
                        "(:keyword IS NULL OR :keyword = '' OR " +
                        " LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> findByAdminCriteria(@Param("categoryId") String categoryId,
                                      @Param("available") Boolean available,
                                      @Param("keyword") String keyword,
                                      Pageable pageable);

    // ── Count orders using this product ──────────────────────────────
    @Query("SELECT COUNT(od) FROM OrderDetail od WHERE od.product.id = :productId")
    long countOrdersByProductId(@Param("productId") Integer productId);

    // ── Home add-on products for homepage section ─────────────────────
    @Query("SELECT p FROM Product p JOIN FETCH p.category " +
           "WHERE p.homeAddon = true AND p.available = true " +
           "ORDER BY COALESCE(p.homeAddonPosition, 999999) ASC, p.createDate DESC, p.id ASC")
    List<Product> findHomeAddonProducts(Pageable pageable);

    long countByCreateDateGreaterThanEqualAndCreateDateLessThan(LocalDateTime from, LocalDateTime to);
}
