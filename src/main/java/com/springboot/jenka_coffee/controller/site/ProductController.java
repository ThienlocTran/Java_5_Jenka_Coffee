package com.springboot.jenka_coffee.controller.site;

import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.service.CategoryService;
import com.springboot.jenka_coffee.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
public class ProductController {

    final ProductService productService;

    final CategoryService categoryService; // Để đổ dữ liệu vào combobox loại hàng

    public ProductController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    // 1. Hiện danh sách sản phẩm (Product List Page - 12 items per page)
    @GetMapping({ "/", "/product/list" })
    public String index(Model model,
            @RequestParam(value = "categoryId", required = false) String categoryId,
            @RequestParam(value = "page", defaultValue = "0") int page) {
        // Pagination: 12 products per page
        Pageable pageable = PageRequest.of(page, 12);
        Page<Product> productPage = productService.filterProductsPaginated(categoryId, pageable);

        model.addAttribute("items", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("currentCategoryId", categoryId);

        return "site/products/product-list"; // Trả về file product-list.html mới
    }

    // 2. Chi tiết sản phẩm
    @GetMapping("/product/detail/{id}")
    public String detail(@PathVariable("id") Integer id, Model model) {
        Map<String, Object> details = productService.getProductDetail(id);
        if (details == null) {
            throw new com.springboot.jenka_coffee.exception.ResourceNotFoundException(
                    "Không tìm thấy sản phẩm với ID: " + id);
        }
        model.addAllAttributes(details);
        return "site/products/product-detail";
    }

    // 3. Quick View API - Returns JSON for fast modal loading
    @GetMapping("/api/product/quick-view/{id}")
    @ResponseBody
    public Map<String, Object> quickView(@PathVariable("id") Integer id) {
        return productService.getProductDetail(id);
    }
}