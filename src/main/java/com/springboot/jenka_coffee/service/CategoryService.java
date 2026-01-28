package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.entity.Category;
import java.util.List;

public interface CategoryService {
    List<Category> findAll();
    Category findById(String id);
    Category save(Category category);
    void delete(String id);
    boolean existsById(String id);
    long countProductsByCategory(String categoryId);
    java.util.Map<String, String> getCategoryIcons();
}