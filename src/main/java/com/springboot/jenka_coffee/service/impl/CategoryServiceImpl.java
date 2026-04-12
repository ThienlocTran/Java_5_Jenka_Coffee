package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.dto.request.CategoryRequest;
import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.exception.BusinessRuleException;
import com.springboot.jenka_coffee.exception.DuplicateResourceException;
import com.springboot.jenka_coffee.exception.ResourceNotFoundException;
import com.springboot.jenka_coffee.repository.CategoryRepository;
import com.springboot.jenka_coffee.repository.ProductRepository;
import com.springboot.jenka_coffee.service.CategoryService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    // 🚨 BUG-61: DATA REDUNDANCY / MISSING DTO PROJECTION (Dư Thừa Dữ Liệu)
    // ================================================================
    // PERFORMANCE ISSUE: Fetching entire Category entity when only 3 fields needed!
    // 
    // Current state:
    // - Method returns List<Category> with ALL fields (id, name, slug, icon, createdDate, etc.)
    // - Frontend menu only needs: id, name, slug (3 fields)
    // - Fetching 10x more data than necessary
    // - Storing bloated entities in cache (wastes RAM)
    // 
    // The problem:
    // When rendering navigation menu:
    // 1. Frontend calls /api/categories
    // 2. Backend fetches entire Category entity (10 columns)
    // 3. Jackson serializes all fields to JSON
    // 4. Network transfers 10x more data
    // 5. Cache stores full entities (wastes memory)
    // 6. Frontend only uses 3 fields, ignores the rest
    // 
    // Example current response (wasteful):
    // ```json
    // [
    //   {
    //     "id": "CF_AN_VAT",
    //     "name": "Cà Phê & Đồ Ăn",
    //     "slug": "ca-phe-do-an",
    //     "icon": "ca_phe_do_an.webp",
    //     "createdDate": "2024-01-15T10:30:00",
    //     "updatedDate": "2024-03-20T14:45:00",
    //     "description": "...",
    //     "displayOrder": 1,
    //     "isActive": true,
    //     "metaKeywords": "..."
    //   }
    // ]
    // ```
    // 
    // Optimized response (what we need):
    // ```json
    // [
    //   {
    //     "id": "CF_AN_VAT",
    //     "name": "Cà Phê & Đồ Ăn",
    //     "slug": "ca-phe-do-an"
    //   }
    // ]
    // ```
    // 
    // Performance impact:
    // - Network: 10KB → 1KB (90% reduction)
    // - Memory: 10 categories × 500 bytes = 5KB → 500 bytes (90% reduction)
    // - Database: Fetches 10 columns → 3 columns (70% less I/O)
    // - JSON parsing: 10 fields → 3 fields (faster serialization)
    // 
    // Solution (NEEDS TO BE IMPLEMENTED):
    // 
    // 1. Create lightweight DTO:
    // ```java
    // public record CategoryMenuDto(String id, String name, String slug) {}
    // ```
    // 
    // 2. Add projection query to CategoryRepository:
    // ```java
    // @Query("SELECT new com.springboot.jenka_coffee.dto.CategoryMenuDto(c.id, c.name, c.slug) " +
    //        "FROM Category c ORDER BY c.displayOrder")
    // List<CategoryMenuDto> getCategoryMenu();
    // ```
    // 
    // 3. Create new service method:
    // ```java
    // @Override
    // @Transactional(readOnly = true)
    // @Cacheable(value = "categoryMenu", sync = true)
    // public List<CategoryMenuDto> getCategoryMenu() {
    //     return categoryRepository.getCategoryMenu();
    // }
    // ```
    // 
    // 4. Update controller to use DTO:
    // ```java
    // @GetMapping("/menu")
    // public ResponseEntity<ApiResponse<List<CategoryMenuDto>>> getCategoryMenu() {
    //     List<CategoryMenuDto> menu = categoryService.getCategoryMenu();
    //     return ResponseEntity.ok(ApiResponse.success("Lấy menu danh mục thành công", menu));
    // }
    // ```
    // 
    // Alternative: Interface-based projection (Spring Data JPA):
    // ```java
    // public interface CategoryMenuProjection {
    //     String getId();
    //     String getName();
    //     String getSlug();
    // }
    // 
    // @Query("SELECT c.id as id, c.name as name, c.slug as slug FROM Category c")
    // List<CategoryMenuProjection> getCategoryMenu();
    // ```
    // 
    // Benefits:
    // - 90% less network bandwidth
    // - 90% less cache memory usage
    // - Faster JSON serialization
    // - Reduced database I/O
    // - Better scalability (can cache more items)
    // 
    // When to use DTO projection:
    // - List/menu endpoints (only need summary data)
    // - Search results (only need key fields)
    // - Autocomplete (only need id + name)
    // - Reports (only need aggregated data)
    // 
    // When to use full entity:
    // - Detail pages (need all fields)
    // - Edit forms (need all fields for update)
    // - Admin management (need complete data)
    // 
    // Related patterns:
    // - GraphQL (client specifies exact fields needed)
    // - JSON:API sparse fieldsets
    // - OData $select parameter
    // 
    // Technical notes:
    // - Constructor expression requires DTO to have matching constructor
    // - Interface projection uses runtime proxy (slight overhead)
    // - Record classes (Java 14+) are perfect for DTOs
    // - Don't over-optimize - only use DTOs for high-traffic endpoints
    // ================================================================
    
    // 🚨 BUG-58: CACHE STAMPEDE / DOGPILE EFFECT (Hiệu Ứng Đàn Sói)
    // ================================================================
    // CRITICAL PERFORMANCE ISSUE: Missing sync=true flag causes cache stampede!
    // 
    // Current state:
    // - Method uses @Cacheable("categories") for performance optimization ✓
    // - Cache works perfectly during normal operation ✓
    // - BUT missing sync=true flag causes catastrophic failure during cache expiration!
    // 
    // The problem:
    // When cache expires during peak hours (e.g., 12:00 PM lunch rush):
    // 1. 1,000 concurrent requests hit this endpoint
    // 2. All 1,000 requests check cache → find it empty (expired)
    // 3. WITHOUT sync=true: ALL 1,000 requests execute categoryRepository.findAll()
    // 4. Database receives 1,000 identical SELECT queries simultaneously
    // 5. Connection pool exhausted (default: 10 connections)
    // 6. Database CPU spikes to 100%
    // 7. Application crashes or becomes unresponsive
    // 
    // This is called "Cache Stampede" or "Dogpile Effect" - a classic distributed systems problem.
    // 
    // The solution (NEEDS TO BE ADDED):
    // Change to: @Cacheable(value = "categories", sync = true)
    // 
    // How sync=true works:
    // 1. Request #1 checks cache → empty → acquires lock → queries database
    // 2. Requests #2-1000 check cache → empty → see lock → WAIT (blocked)
    // 3. Request #1 finishes → stores result in cache → releases lock
    // 4. Requests #2-1000 wake up → check cache → find data → return from RAM
    // 
    // Performance comparison:
    // WITHOUT sync=true:
    // - Cache hit: 1ms (from RAM) ✓
    // - Cache miss: 1,000 × 50ms = 50,000ms total DB load ✗
    // - Database: 1,000 queries × 10 rows = 10,000 rows scanned ✗
    // 
    // WITH sync=true:
    // - Cache hit: 1ms (from RAM) ✓
    // - Cache miss: 1 × 50ms + 999 × 2ms = 2,048ms total ✓
    // - Database: 1 query × 10 rows = 10 rows scanned ✓
    // 
    // Business impact:
    // - Website crashes during peak hours (worst possible timing!)
    // - Lost sales during lunch/dinner rush
    // - Poor user experience (timeouts, errors)
    // - Database costs spike (cloud providers charge per query)
    // - Cannot scale to production traffic levels
    // 
    // Technical notes:
    // - sync=true uses internal lock (not distributed across servers)
    // - For multi-server deployment, consider Redis distributed lock
    // - Cache expiration should be staggered (not all at same time)
    // - Monitor cache hit ratio (should be >95% for this endpoint)
    // - Consider cache warming strategy (pre-populate before expiration)
    // 
    // Related Spring Boot documentation:
    // https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/cache/annotation/Cacheable.html#sync--
    // 
    // "When set to true, only one thread will be allowed to call the method 
    // when the cache is being populated. Other threads will be blocked until 
    // the cache is populated."
    // ================================================================
    @Override
    @Transactional(readOnly = true)
    @Cacheable("categories")
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Category> findAllPaginated(Pageable pageable) {
        return categoryRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Category findById(String id) {
        return categoryRepository.findById(id).orElse(null);
    }

    @Override
    @CacheEvict(value = "categories", allEntries = true)
    public Category save(Category category) {
        return categoryRepository.save(category);
    }

    @Override
    @CacheEvict(value = "categories", allEntries = true)
    public void delete(String id) {
        categoryRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(String id) {
        return categoryRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long countProductsByCategory(String categoryId) {
        return productRepository.countByCategoryId(categoryId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getCategoryIcons() {
        Map<String, String> icons = new java.util.HashMap<>();
        icons.put("CF_AN_VAT", "ca_phe_do_an.webp");
        icons.put("DUNG_CU", "dung_cu_pha_che.webp");
        icons.put("HANG_CU", "may_pha_may_xay_cu.webp");
        icons.put("MAY_PHA", "May_Pha_Ca_Phe.webp");
        icons.put("MAY_XAY", "May_Xay_Ca_Phe.webp");
        icons.put("XAY_EP", "may_xay_sinh_to_may_ep.webp");
        return icons;
    }

    @Override
    @Transactional(readOnly = true)
    public Category findByIdOrThrow(String id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category", "id", id));
    }

    @Override
    @CacheEvict(value = "categories", allEntries = true)
    public void deleteOrThrow(String id) {
        // Verify category exists (throws exception if not found)
        findByIdOrThrow(id);

        long productCount = countProductsByCategory(id);
        if (productCount > 0) {
            throw new BusinessRuleException(
                    "Không thể xóa loại hàng này vì còn " + productCount + " sản phẩm thuộc loại này!");
        }

        categoryRepository.deleteById(id);
    }

    @Override
    @CacheEvict(value = "categories", allEntries = true)
    public Category createCategory(CategoryRequest request) {
        // Normalize data
        request.normalize();

        // Check duplicate
        if (categoryRepository.existsById(request.getId())) {
            throw new DuplicateResourceException(
                    "Category", "id", request.getId());
        }

        Category category = request.toEntity();

        // Set icon from predefined list if not provided
        if (category.getIcon() == null || category.getIcon().isEmpty()) {
            Map<String, String> icons = getCategoryIcons();
            category.setIcon(icons.get(category.getId()));
        }

        return categoryRepository.save(category);
    }

    @Override
    @CacheEvict(value = "categories", allEntries = true)
    public Category updateCategory(String id, CategoryRequest request) {
        Category existing = findByIdOrThrow(id);

        // Normalize data
        request.normalize();

        // Update fields from request
        existing.setName(request.getName());

        // Update icon if provided, otherwise keep existing or set from predefined list
        if (request.getIcon() != null && !request.getIcon().isEmpty()) {
            existing.setIcon(request.getIcon());
        } else if (existing.getIcon() == null || existing.getIcon().isEmpty()) {
            Map<String, String> icons = getCategoryIcons();
            existing.setIcon(icons.get(existing.getId()));
        }

        return categoryRepository.save(existing);
    }
}