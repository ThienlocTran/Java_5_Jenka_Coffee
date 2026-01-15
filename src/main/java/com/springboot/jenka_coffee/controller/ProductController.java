package com.springboot.jenka_coffee.controller;

import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.service.CategoryService;
import com.springboot.jenka_coffee.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
        List<Product> list = productService.filterProducts(categoryId);
        model.addAttribute("items", list);
        model.addAttribute("currentCategoryId", categoryId);
        return "site/products/product-list"; // Trả về file product-list.html mới
    }

    // 2. Chi tiết sản phẩm
    @GetMapping("/product/detail/{id}")
    public String detail(@PathVariable("id") Integer id, Model model) {
        Map<String, Object> details = productService.getProductDetail(id);
        if (details == null) {
            return "redirect:/home";
        }
        model.addAllAttributes(details);
        return "site/products/product-detail";
    }
}