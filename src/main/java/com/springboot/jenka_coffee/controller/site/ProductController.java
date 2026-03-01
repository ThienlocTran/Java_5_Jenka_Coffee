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

import java.math.BigDecimal;
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
        
        // Add categories and counts for sidebar
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("categoryCounts", productService.getCategoryCounts());

        return "site/products/product-list"; // Trả về file product-list.html mới
    }

    // 1.1. Lọc sản phẩm nâng cao (theo loại + giá)
    @GetMapping("/product/filter")
    public String filterProducts(Model model,
            @RequestParam(value = "categoryId", required = false) String categoryId,
            @RequestParam(value = "minPrice", required = false) Double minPriceDouble,
            @RequestParam(value = "maxPrice", required = false) Double maxPriceDouble,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page) {
        
        // Convert Double to BigDecimal for proper comparison with DB
        BigDecimal minPrice = minPriceDouble != null ? BigDecimal.valueOf(minPriceDouble) : null;
        BigDecimal maxPrice = maxPriceDouble != null ? BigDecimal.valueOf(maxPriceDouble) : null;
        
        // Pagination: 12 products per page
        Pageable pageable = PageRequest.of(page, 12);
        Page<Product> productPage = productService.filterProductsWithAllCriteria(categoryId, minPrice, maxPrice, keyword, pageable);

        model.addAttribute("items", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("currentCategoryId", categoryId);
        model.addAttribute("currentMinPrice", minPriceDouble);
        model.addAttribute("currentMaxPrice", maxPriceDouble);
        model.addAttribute("currentKeyword", keyword);
        
        // Add categories and counts for sidebar
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("categoryCounts", productService.getCategoryCounts());

        return "site/products/product-list";
    }

    // 1.2. Tìm kiếm sản phẩm
    @GetMapping("/product/search")
    public String searchProducts(Model model,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page) {
        
        // Pagination: 12 products per page
        Pageable pageable = PageRequest.of(page, 12);
        Page<Product> productPage = productService.searchProductsPaginated(keyword, pageable);

        model.addAttribute("items", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("currentKeyword", keyword);
        
        // Add categories and counts for sidebar
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("categoryCounts", productService.getCategoryCounts());

        return "site/products/product-list";
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