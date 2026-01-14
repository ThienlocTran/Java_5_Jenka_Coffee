package com.springboot.jenka_coffee.controller;

import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.service.CategoryService;
import com.springboot.jenka_coffee.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class ProductController {

    final ProductService productService;

    final CategoryService categoryService; // Để đổ dữ liệu vào combobox loại hàng

    public ProductController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    // 1. Hiện danh sách sản phẩm (Trang chủ)
    @GetMapping({ "/", "/product/list" })
    public String index(Model model, @RequestParam(value = "categoryId", required = false) String categoryId) {
        List<Product> list;
        if (categoryId != null && !categoryId.isEmpty()) {
            list = productService.findByCategoryId(categoryId);
            model.addAttribute("currentCategoryId", categoryId);
        } else {
            list = productService.findAll();
        }
        model.addAttribute("items", list);
        return "site/products/product-list"; // Trả về file product-list.html mới
    }
}