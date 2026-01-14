package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.repository.CategoryDAO; // Nhớ tạo Interface DAO extend JpaRepository nhé
import com.springboot.jenka_coffee.service.CategoryService;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    final CategoryDAO cdao;

    public CategoryServiceImpl(CategoryDAO cdao) {
        this.cdao = cdao;
    }

    @Override
    public List<Category> findAll() {
        return cdao.findAll();
    }

    @Override
    public java.util.Map<String, String> getCategoryIcons() {
        java.util.Map<String, String> icons = new java.util.HashMap<>();
        icons.put("CF_AN_VAT", "ca_phe_do_an.webp");
        icons.put("DUNG_CU", "dung_cu_pha_che.webp");
        icons.put("HANG_CU", "may_pha_may_xay_cu.webp");
        icons.put("MAY_PHA", "May_Pha_Ca_Phe.webp");
        icons.put("MAY_XAY", "May_Xay_Ca_Phe.webp");
        icons.put("XAY_EP", "may_xay_sinh_to_may_ep.webp");
        return icons;
    }
}