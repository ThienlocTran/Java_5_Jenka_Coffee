package com.springboot.jenka_coffee.controller;

import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.service.CategoryService;
import com.springboot.jenka_coffee.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    public String index(Model model) {
        List<Product> list = productService.findAll();
        model.addAttribute("items", list);
        return "product/list"; // Trả về file list.html
    }

    // 2. Mở form thêm mới
    @GetMapping("/product/create")
    public String create(Model model) {
        Product p = new Product();
        model.addAttribute("item", p); // Gửi đối tượng rỗng sang form

        List<Category> categories = categoryService.findAll();
        model.addAttribute("categories", categories); // Gửi list loại để chọn

        return "product/form"; // Trả về file form.html
    }

    // 3. Mở form chỉnh sửa (Load dữ liệu cũ lên)
    @GetMapping("/product/edit/{id}")
    public String edit(@PathVariable("id") Integer id, Model model) {
        Product p = productService.findById(id);
        model.addAttribute("item", p); // Gửi đối tượng cũ sang form

        List<Category> categories = categoryService.findAll();
        model.addAttribute("categories", categories);

        return "product/form"; // Dùng chung form với create
    }

    // 4. Xử lý nút LƯU (Dùng chung cho cả Thêm và Sửa)
    @PostMapping("/product/save")
    public String save(
            @ModelAttribute("item") Product product, // Hứng dữ liệu từ form
            @RequestParam("photo_file") MultipartFile file // Hứng file ảnh
    ) {
        // --- XỬ LÝ ẢNH VÀ LƯU DB (LOGIC TRONG SERVICE) ---
        productService.saveProduct(product, file);

        return "redirect:/product/list"; // Lưu xong quay về trang danh sách
    }

    // 5. Xóa sản phẩm
    @GetMapping("/product/delete/{id}")
    public String delete(@PathVariable("id") Integer id) {
        productService.delete(id);
        return "redirect:/product/list";
    }
}