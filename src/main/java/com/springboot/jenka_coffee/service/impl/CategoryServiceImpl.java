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