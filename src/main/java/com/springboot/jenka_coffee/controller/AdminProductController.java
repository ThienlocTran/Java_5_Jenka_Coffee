package com.springboot.jenka_coffee.controller;

import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.service.CategoryService;
import com.springboot.jenka_coffee.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequestMapping("/admin/product")
public class AdminProductController {

    final ProductService productService;
    final CategoryService categoryService;

    public AdminProductController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    // 1. Admin List View
    @GetMapping("/list")
    public String index(Model model) {
        List<Product> list = productService.findAll();
        model.addAttribute("items", list);
        return "admin/products/list"; // Admin table view
    }

    // 2. Create Form
    @GetMapping("/create")
    public String create(Model model) {
        Product p = new Product();
        model.addAttribute("item", p);
        model.addAttribute("categories", categoryService.findAll());
        return "admin/products/form";
    }

    // 3. Edit Form
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Integer id, Model model) {
        Product p = productService.findById(id);
        model.addAttribute("item", p);
        model.addAttribute("categories", categoryService.findAll());
        return "admin/products/form";
    }

    // 4. Save Action
    @PostMapping("/save")
    public String save(@ModelAttribute("item") Product product,
            @RequestParam(value = "imageFile", required = false) MultipartFile file) {
        productService.saveProduct(product, file);
        return "redirect:/admin/product/list";
    }

    // 5. Toggle Available (Soft Delete)
    @GetMapping("/toggle/{id}")
    public String toggleAvailable(@PathVariable("id") Integer id) {
        Product product = productService.findById(id);
        if (product != null) {
            product.setAvailable(!product.getAvailable());
            productService.update(product);
        }
        return "redirect:/admin/product/list";
    }
}
