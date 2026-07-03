package com.springboot.jenka_coffee.api;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.entity.ProductImage;
import com.springboot.jenka_coffee.entity.ProductKind;
import com.springboot.jenka_coffee.exception.ResourceNotFoundException;
import com.springboot.jenka_coffee.service.ProductService;
import com.springboot.jenka_coffee.util.SqlUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Public Product API Controller - Clean 3-Tier
 * Controller: HTTP only, no @Transactional, no business logic
 */
@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ApiProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProducts(
            @RequestParam(value = "categoryId", required = false) String categoryId,
            @RequestParam(value = "categorySlug", required = false) String categorySlug,
            @RequestParam(value = "productKind", required = false) String productKindParam,
            @RequestParam(value = "minPrice", required = false) Double minPriceDouble,
            @RequestParam(value = "maxPrice", required = false) Double maxPriceDouble,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sort", defaultValue = "newest") String sort,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "12") int size) {

        // VULN-DATABASE-DOS FIX: Giới hạn độ dài keyword để tránh heavy LIKE query
        if (keyword != null && keyword.length() > 100) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Từ khóa tìm kiếm quá dài (tối đa 100 ký tự)"));
        }
        
        // VULN-SQL-INJECTION FIX: Escape SQL wildcards trong keyword
        if (keyword != null && !keyword.isEmpty()) {
            keyword = SqlUtils.sanitizeSearchInput(keyword, 100);
        }

        // VULN-DEEP-PAGINATION-DOS FIX: Giới hạn page number để tránh OFFSET DoS
        // Giới hạn page tối đa 1000 (50 items/page = 50,000 items max)
        if (page > 1000) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Số trang vượt quá giới hạn (tối đa 1000)"));
        }

        // Giới hạn cứng — tránh OOM khi client gửi size=999999
        size = Math.min(Math.max(size, 1), 50);
        page = Math.max(page, 0);

        BigDecimal minPrice = minPriceDouble != null ? BigDecimal.valueOf(minPriceDouble) : null;
        BigDecimal maxPrice = maxPriceDouble != null ? BigDecimal.valueOf(maxPriceDouble) : null;
        ProductKind productKind;
        try {
            productKind = ProductKind.fromNullable(productKindParam);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("productKind không hợp lệ"));
        }

        Sort sortOrder = switch (sort) {
            case "price_asc"  -> Sort.by(Sort.Order.asc("price").nullsLast());
            case "price_desc" -> Sort.by(Sort.Order.desc("price").nullsLast());
            case "name_asc"   -> Sort.by("name").ascending();
            default           -> Sort.by("id").descending(); // newest
        };

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<Product> productPage = productService.filterProductsWithAllCriteria(categoryId, categorySlug, productKind, minPrice, maxPrice,
                keyword, pageable);

        // BUG FIX: Add null check for productPage to prevent NPE
        if (productPage == null) {
            log.error("Service returned null productPage for filters: categoryId={}, minPrice={}, maxPrice={}, keyword={}", 
                    categoryId, minPrice, maxPrice, keyword);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Lỗi khi lấy danh sách sản phẩm"));
        }

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("items", productPage.getContent());
        responseData.put("currentPage", productPage.getNumber());
        responseData.put("totalItems", productPage.getTotalElements());
        responseData.put("totalPages", productPage.getTotalPages());

        return ResponseEntity.ok(ApiResponse.success("Products fetched successfully", responseData));
    }

    @GetMapping("/home-addons")
    public ResponseEntity<ApiResponse<List<Product>>> getHomeAddonProducts(
            @RequestParam(value = "limit", defaultValue = "12") int limit) {
        try {
            limit = Math.min(Math.max(limit, 1), 50);
            List<Product> items = productService.getHomeAddonProducts(limit);
            return ResponseEntity.ok(ApiResponse.success("Home addon products fetched successfully", items));
        } catch (Exception e) {
            log.error("Error getting home addon products", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Lỗi khi lấy sản phẩm bán kèm"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProductDetail(@PathVariable("id") Integer id) {
        try {
            Map<String, Object> details = productService.getProductDetail(id);
            return ResponseEntity.ok(ApiResponse.success("Product detail fetched successfully", details));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(ApiResponse.error("Không tìm thấy sản phẩm với ID: " + id));
        } catch (Exception e) {
            log.error("Error getting product detail: {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Lỗi khi lấy thông tin sản phẩm"));
        }
    }
    
    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProductDetailBySlug(@PathVariable("slug") String slug) {
        try {
            Product product = productService.findBySlug(slug);
            List<Product> similarItems = productService.getRelatedProducts(
                    product.getCategory().getId(),
                product.getId()
            );
            
            Map<String, Object> details = new HashMap<>();
            details.put("item", product);
            details.put("similarItems", similarItems);
            
            return ResponseEntity.ok(ApiResponse.success("Product detail fetched successfully", details));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(ApiResponse.error("Không tìm thấy sản phẩm với slug: " + slug));
        } catch (Exception e) {
            log.error("Error getting product by slug: {}", slug, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Lỗi khi lấy thông tin sản phẩm"));
        }
    }

    @GetMapping("/counts")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getCategoryCounts() {
        try {
            return ResponseEntity.ok(ApiResponse.success("Category counts fetched successfully", 
                productService.getCategoryCounts()));
        } catch (Exception e) {
            log.error("Error getting category counts", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Lỗi khi lấy số lượng danh mục"));
        }
    }

    @GetMapping("/{id}/images")
    public ResponseEntity<ApiResponse<List<ProductImage>>> getProductImages(@PathVariable Integer id) {
        try {
            List<ProductImage> images = productService.getProductImages(id);
            return ResponseEntity.ok(ApiResponse.success("Product images fetched successfully", images));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(ApiResponse.error("Không tìm thấy sản phẩm"));
        } catch (Exception e) {
            log.error("Error fetching product images: {}", id, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Error fetching product images: " + e.getMessage()));
        }
    }
}
