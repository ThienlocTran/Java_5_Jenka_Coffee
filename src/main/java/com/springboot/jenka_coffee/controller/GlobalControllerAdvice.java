package com.springboot.jenka_coffee.controller;

import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.service.CategoryService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;
import java.util.Map;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final CategoryService categoryService;

    public GlobalControllerAdvice(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @ModelAttribute("categories")
    public List<Category> populateCategories() {
        return categoryService.findAll();
    }

    @ModelAttribute("categoryIcons")
    public Map<String, String> populateCategoryIcons() {
        return categoryService.getCategoryIcons();
    }
}
