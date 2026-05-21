package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.dto.request.CategoryRequest;
import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.exception.BusinessRuleException;
import com.springboot.jenka_coffee.exception.DuplicateResourceException;
import com.springboot.jenka_coffee.exception.ResourceNotFoundException;
import com.springboot.jenka_coffee.repository.CategoryRepository;
import com.springboot.jenka_coffee.repository.ProductRepository;
import com.springboot.jenka_coffee.service.CategoryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
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

    private static final Logger logger = LoggerFactory.getLogger(CategoryServiceImpl.class);

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }
    //  BUG-61: DATA REDUNDANCY / MISSING DTO PROJECTION (Dư Thừa Dữ Liệu)

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
        request.validateImageDisplay();
        String normalizedId = request.getId();
        String normalizedSlug = request.getSlug();

        if (categoryRepository.existsById(normalizedId)) {
            throw new DuplicateResourceException("Mã loại hàng đã tồn tại: " + normalizedId);
        }
        if (normalizedSlug != null && !normalizedSlug.isBlank()
                && categoryRepository.existsBySlugIgnoreCase(normalizedSlug)) {
            throw new DuplicateResourceException("Slug SEO đã tồn tại: " + normalizedSlug);
        }

        Category category = request.toEntity();

        // Set icon from predefined list if not provided
        if (category.getIcon() == null || category.getIcon().isEmpty()) {
            Map<String, String> icons = getCategoryIcons();
            category.setIcon(icons.get(category.getId()));
        }

        try {
            return categoryRepository.save(category);
        } catch (DataIntegrityViolationException ex) {
            String constraintName = extractConstraintName(ex);
            logger.warn("Category create data integrity violation (constraint={}): {}", constraintName, ex.getMessage());
            if (isCategoryIdConstraint(constraintName, ex)) {
                throw new DuplicateResourceException("Mã loại hàng đã tồn tại: " + normalizedId);
            }
            if (isCategorySlugConstraint(constraintName, ex)) {
                throw new DuplicateResourceException("Slug SEO đã tồn tại: " + normalizedSlug);
            }
            String missingField = extractNotNullColumn(ex);
            if (missingField != null) {
                throw new IllegalArgumentException("Lỗi dữ liệu danh mục: " + missingField + " không được để trống");
            }
            throw new IllegalArgumentException(
                    "Vi phạm ràng buộc dữ liệu categories"
                            + (constraintName == null ? "" : ": " + constraintName));
        }
    }

    @Override
    @CacheEvict(value = "categories", allEntries = true)
    public Category updateCategory(String id, CategoryRequest request) {
        Category existing = findByIdOrThrow(id);
        boolean hasImageCropX = request.getImageCropX() != null;
        boolean hasImageCropY = request.getImageCropY() != null;
        boolean hasImageCropWidth = request.getImageCropWidth() != null;
        boolean hasImageCropHeight = request.getImageCropHeight() != null;
        boolean hasImageZoom = request.getImageZoom() != null;

        // Normalize data
        request.normalize();

        // Update fields from request
        existing.setName(request.getName());
        existing.setSlug(request.getSlug());

        // Update icon if provided, otherwise keep existing or set from predefined list
        if (request.getIcon() != null && !request.getIcon().isEmpty()) {
            existing.setIcon(request.getIcon());
        } else if (existing.getIcon() == null || existing.getIcon().isEmpty()) {
            Map<String, String> icons = getCategoryIcons();
            existing.setIcon(icons.get(existing.getId()));
        }

        request.validateImageDisplay();
        existing.setImageCropX(resolveCropValue(
                hasImageCropX ? request.getImageCropX() : existing.getImageCropX(),
                java.math.BigDecimal.ZERO));
        existing.setImageCropY(resolveCropValue(
                hasImageCropY ? request.getImageCropY() : existing.getImageCropY(),
                java.math.BigDecimal.ZERO));
        existing.setImageCropWidth(resolveCropValue(
                hasImageCropWidth ? request.getImageCropWidth() : existing.getImageCropWidth(),
                new java.math.BigDecimal("100.00")));
        existing.setImageCropHeight(resolveCropValue(
                hasImageCropHeight ? request.getImageCropHeight() : existing.getImageCropHeight(),
                new java.math.BigDecimal("100.00")));
        existing.setImageZoom(resolveCropValue(
                hasImageZoom ? request.getImageZoom() : existing.getImageZoom(),
                new java.math.BigDecimal("1.00")));

        try {
            return categoryRepository.save(existing);
        } catch (DataIntegrityViolationException ex) {
            String constraintName = extractConstraintName(ex);
            logger.warn("Category update data integrity violation (constraint={}): {}", constraintName, ex.getMessage());
            if (isCategoryIdConstraint(constraintName, ex)) {
                throw new DuplicateResourceException("Mã loại hàng đã tồn tại: " + existing.getId());
            }
            if (isCategorySlugConstraint(constraintName, ex)) {
                throw new DuplicateResourceException("Slug SEO đã tồn tại: " + existing.getSlug());
            }
            String missingField = extractNotNullColumn(ex);
            if (missingField != null) {
                throw new IllegalArgumentException("Lỗi dữ liệu danh mục: " + missingField + " không được để trống");
            }
            throw new IllegalArgumentException(
                    "Vi phạm ràng buộc dữ liệu categories"
                            + (constraintName == null ? "" : ": " + constraintName));
        }
    }

    private String extractConstraintName(DataIntegrityViolationException exception) {
        Throwable current = exception;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase();
                if (normalized.contains("categories_pkey")) return "categories_pkey";
                if (normalized.contains("ux_categories_slug")) return "ux_categories_slug";
                if (normalized.contains("categories_slug_key")) return "categories_slug_key";
            }
            current = current.getCause();
        }
        return null;
    }

    private boolean isCategoryIdConstraint(String constraintName, DataIntegrityViolationException exception) {
        return "categories_pkey".equals(constraintName)
                || containsConstraintText(exception, "categories_pkey");
    }

    private boolean isCategorySlugConstraint(String constraintName, DataIntegrityViolationException exception) {
        return "ux_categories_slug".equals(constraintName)
                || "categories_slug_key".equals(constraintName)
                || containsConstraintText(exception, "ux_categories_slug")
                || containsConstraintText(exception, "categories_slug_key");
    }

    private boolean containsConstraintText(DataIntegrityViolationException exception, String expected) {
        Throwable current = exception;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains(expected.toLowerCase())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String extractNotNullColumn(DataIntegrityViolationException exception) {
        Throwable current = exception;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String lower = message.toLowerCase();
                if (lower.contains("null value in column")) {
                    int start = message.indexOf('"');
                    int end = start >= 0 ? message.indexOf('"', start + 1) : -1;
                    if (start >= 0 && end > start) {
                        return message.substring(start + 1, end);
                    }
                }
            }
            current = current.getCause();
        }
        return null;
    }

    private java.math.BigDecimal resolveCropValue(java.math.BigDecimal value, java.math.BigDecimal fallback) {
        return value != null ? value : fallback;
    }
}
