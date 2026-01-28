package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.repository.CategoryDAO; // Nhớ tạo Interface DAO extend JpaRepository nhé
import com.springboot.jenka_coffee.repository.ProductDAO;
import com.springboot.jenka_coffee.service.CategoryService;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryDAO categoryDAO;
    private final ProductDAO productDAO;

    public CategoryServiceImpl(CategoryDAO categoryDAO, ProductDAO productDAO) {
        this.categoryDAO = categoryDAO;
        this.productDAO = productDAO;
    }

    @Override
    public List<Category> findAll() {
        return categoryDAO.findAll();
    }

    @Override
    public Category findById(String id) {
        return categoryDAO.findById(id).orElse(null);
    }

    @Override
    public Category save(Category category) {
        return categoryDAO.save(category);
    }

    @Override
    public void delete(String id) {
        categoryDAO.deleteById(id);
    }

    @Override
    public boolean existsById(String id) {
        return categoryDAO.existsById(id);
    }

    @Override
    public long countProductsByCategory(String categoryId) {
        return productDAO.countByCategoryId(categoryId);
    }

    @Override
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
}