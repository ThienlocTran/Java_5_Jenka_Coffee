package com.springboot.jenka_coffee.api;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.entity.ProductImage;
import com.springboot.jenka_coffee.service.ProductService;
import com.springboot.jenka_coffee.service.impl.ProductServiceImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ApiProductController {

    private final ProductService productService;


    public ApiProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProducts(
            @RequestParam(value = "categoryId", required = false) String categoryId,
            @RequestParam(value = "minPrice", required = false) Double minPriceDouble,
            @RequestParam(value = "maxPrice", required = false) Double maxPriceDouble,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sort", defaultValue = "newest") String sort,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "12") int size) {

        // Giới hạn cứng — tránh OOM khi client gửi size=999999
        size = Math.min(Math.max(size, 1), 50);
        page = Math.max(page, 0);

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
    @Transactional(readOnly = true)
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

    @GetMapping("/{id}/images")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<ProductImage>>> getProductImages(@PathVariable Integer id) {
        try {
            List<ProductImage> images = ((ProductServiceImpl) productService).getProductImages(id);
            return ResponseEntity.ok(ApiResponse.success("Product images fetched successfully", images));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Error fetching product images: " + e.getMessage()));
        }
    }
}
