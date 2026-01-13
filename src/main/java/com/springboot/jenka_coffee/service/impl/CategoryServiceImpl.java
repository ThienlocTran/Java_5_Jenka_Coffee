package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.repository.CategoryDAO; // Nhớ tạo Interface DAO extend JpaRepository nhé
import com.springboot.jenka_coffee.service.CategoryService;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    final
    CategoryDAO cdao;

    public CategoryServiceImpl(CategoryDAO cdao) {
        this.cdao = cdao;
    }

    @Override
    public List<Category> findAll() {
        return cdao.findAll();
    }
}