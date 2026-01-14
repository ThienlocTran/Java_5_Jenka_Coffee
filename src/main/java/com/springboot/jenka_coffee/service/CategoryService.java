package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.entity.Category;
import java.util.List;

public interface CategoryService {
    List<Category> findAll();

    java.util.Map<String, String> getCategoryIcons();
}