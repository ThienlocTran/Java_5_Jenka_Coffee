package com.springboot.jenka_coffee.controller;

import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.service.CategoryService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final CategoryService categoryService;

    public GlobalControllerAdvice(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @ModelAttribute("categories")
    public java.util.List<com.springboot.jenka_coffee.entity.Category> populateCategories() {
        return categoryService.findAll();
    }

    @ModelAttribute("categoryIcons")
    public java.util.Map<String, String> populateCategoryIcons() {
        return categoryService.getCategoryIcons();
    }
}
