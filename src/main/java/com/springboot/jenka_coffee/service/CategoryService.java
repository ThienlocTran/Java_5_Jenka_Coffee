package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.dto.request.CategoryRequest;
import com.springboot.jenka_coffee.entity.Category;
import java.util.List;
import java.util.Map;

public interface CategoryService {
    List<Category> findAll();

    Category findById(String id);

    Category save(Category category);

    void delete(String id);

    boolean existsById(String id);

    long countProductsByCategory(String categoryId);

    Map<String, String> getCategoryIcons();

    // New methods for clean controller pattern
    /**
     * Find category by ID, throws ResourceNotFoundException if not found
     */
    Category findByIdOrThrow(String id);

    /**
     * Delete category with validation, throws BusinessRuleException if has products
     */
    void deleteOrThrow(String id);

    /**
     * Create new category from DTO with validation
     */
    Category createCategory(CategoryRequest request);

    /**
     * Update existing category from DTO
     */
    Category updateCategory(String id, CategoryRequest request);
}