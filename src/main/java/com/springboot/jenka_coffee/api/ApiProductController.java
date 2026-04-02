package com.springboot.jenka_coffee.api;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ApiProductController {

    private final ProductService productService;


    public ApiProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProducts(
            @RequestParam(value = "categoryId", required = false) String categoryId,
            @RequestParam(value = "minPrice", required = false) Double minPriceDouble,
            @RequestParam(value = "maxPrice", required = false) Double maxPriceDouble,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sort", defaultValue = "newest") String sort,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "12") int size) {

        BigDecimal minPrice = minPriceDouble != null ? BigDecimal.valueOf(minPriceDouble) : null;
        BigDecimal maxPrice = maxPriceDouble != null ? BigDecimal.valueOf(maxPriceDouble) : null;

        org.springframework.data.domain.Sort sortOrder = switch (sort) {
            case "price_asc"  -> org.springframework.data.domain.Sort.by("price").ascending();
            case "price_desc" -> org.springframework.data.domain.Sort.by("price").descending();
            case "name_asc"   -> org.springframework.data.domain.Sort.by("name").ascending();
            default           -> org.springframework.data.domain.Sort.by("id").descending(); // newest
        };

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<Product> productPage = productService.filterProductsWithAllCriteria(categoryId, minPrice, maxPrice,
                keyword, pageable);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("items", productPage.getContent());
        responseData.put("currentPage", productPage.getNumber());
        responseData.put("totalItems", productPage.getTotalElements());
        responseData.put("totalPages", productPage.getTotalPages());

        return ResponseEntity.ok(ApiResponse.success("Products fetched successfully", responseData));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProductDetail(@PathVariable("id") Integer id) {
        Map<String, Object> details = productService.getProductDetail(id);
        if (details == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Không tìm thấy sản phẩm với ID: " + id));
        }
        return ResponseEntity.ok(ApiResponse.success("Product detail fetched successfully", details));
    }

    @GetMapping("/counts")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getCategoryCounts() {
        return ResponseEntity
                .ok(ApiResponse.success("Category counts fetched successfully", productService.getCategoryCounts()));
    }
}
