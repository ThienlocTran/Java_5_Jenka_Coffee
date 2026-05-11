package com.springboot.jenka_coffee.api.admin;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.dto.request.ProductRequest;
import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.entity.ProductImage;
import com.springboot.jenka_coffee.exception.BusinessRuleException;
import com.springboot.jenka_coffee.exception.ResourceNotFoundException;
import com.springboot.jenka_coffee.service.ProductService;
import com.springboot.jenka_coffee.validator.ProductValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin Product Controller - Clean 3-Tier Architecture
 * LAYER 1: Controller (This class)
 * - Nhận HTTP request
 * - Validate input cơ bản
 * - Gọi Service
 * - Trả HTTP response
 * - KHÔNG có business logic
 * - KHÔNG có @Transactional
 * LAYER 2: Service (ProductService)
 * - Business logic
 * - Validation phức tạp
 * - Transaction management (@Transactional)
 * - Orchestration (gọi nhiều repository)
 * LAYER 3: Repository (ProductRepository)
 * - Data access
 * - CRUD operations
 * - Custom queries
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class ApiAdminProductController {

    private final ProductService productService;
    private final ProductValidator productValidator;

    /**
     * GET /api/admin/products?page=0&size=10
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> listProducts(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        
        try {
            // Validate pagination
            size = Math.min(Math.max(size, 1), 100);
            page = Math.max(page, 0);

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createDate"));
            Page<Product> productPage = productService.findAllPaginated(pageable);

            Map<String, Object> data = new HashMap<>();
            data.put("items", productPage.getContent());
            data.put("currentPage", productPage.getNumber());
            data.put("totalPages", productPage.getTotalPages());
            data.put("totalItems", productPage.getTotalElements());

            return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm thành công", data));
        } catch (Exception e) {
            log.error("Error listing products", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi lấy danh sách sản phẩm"));
        }
    }

    /**
     * GET /api/admin/products/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> getProduct(@PathVariable Integer id) {
        try {
            Product product = productService.findById(id);
            return ResponseEntity.ok(ApiResponse.success("Lấy thông tin sản phẩm thành công", product));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting product: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi lấy thông tin sản phẩm"));
        }
    }

    /**
     * POST /api/admin/products
     * Create new product
     */
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<Product>> createProduct(
            @ModelAttribute ProductRequest request,
            @RequestParam("categoryId") String categoryId,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {
        
        try {
            // Validate image file
            productValidator.validateImageFile(imageFile);
            
            // Delegate to service
            Product product = productService.createProductFromRequest(request, categoryId, imageFile);
            return ResponseEntity.ok(ApiResponse.success("Thêm sản phẩm thành công", product));
        } catch (BusinessRuleException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating product", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi tạo sản phẩm: " + e.getMessage()));
        }
    }

    /**
     * PUT /api/admin/products/{id}
     * Update product
     * FIX BUG: price luôn null - giờ validate đúng ở Service layer
     */
    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<Product>> updateProductPut(
            @PathVariable Integer id,
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("price") BigDecimal price,
            @RequestParam("categoryId") String categoryId,
            @RequestParam("available") Boolean available,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {
        
        try {
            // Validate image file
            productValidator.validateImageFile(imageFile);
            
            // Delegate to service - Service sẽ validate price
            Product product = productService.updateProductFromRequest(
                id, name, description, price, categoryId, available, imageFile
            );
            return ResponseEntity.ok(ApiResponse.success("Cập nhật sản phẩm thành công", product));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (BusinessRuleException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating product: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi cập nhật sản phẩm: " + e.getMessage()));
        }
    }

    /**
     * POST /api/admin/products/{id}
     * Update product (POST for compatibility)
     */
    @PostMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<Product>> updateProductPost(
            @PathVariable Integer id,
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("price") BigDecimal price,
            @RequestParam("categoryId") String categoryId,
            @RequestParam("available") Boolean available,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {
        
        return updateProductPut(id, name, description, price, categoryId, available, imageFile);
    }

    /**
     * PUT /api/admin/products/{id}/toggle
     */
    @PutMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<Void>> toggleAvailable(@PathVariable Integer id) {
        try {
            productService.toggleAvailable(id);
            return ResponseEntity.ok(ApiResponse.success("Đổi trạng thái sản phẩm thành công", null));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error toggling product availability: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi đổi trạng thái sản phẩm"));
        }
    }

    /**
     * PUT /api/admin/products/{id}/toggle-featured
     */
    @PutMapping("/{id}/toggle-featured")
    public ResponseEntity<ApiResponse<Product>> toggleFeatured(@PathVariable Integer id) {
        try {
            Product product = productService.toggleFeatured(id);
            String message = Boolean.TRUE.equals(product.getFeatured()) 
                ? "Đã đánh dấu sản phẩm nổi bật" 
                : "Đã bỏ đánh dấu sản phẩm nổi bật";
            return ResponseEntity.ok(ApiResponse.success(message, product));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error toggling product featured: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi đổi trạng thái nổi bật"));
        }
    }

    /**
     * PUT /api/admin/products/{id}/featured-position
     * Body: { "position": 1 } to pin on homepage, or null/0 to remove.
     */
    @PutMapping("/{id}/featured-position")
    public ResponseEntity<ApiResponse<Product>> updateFeaturedPosition(
            @PathVariable Integer id,
            @RequestBody Map<String, Integer> body) {
        try {
            Integer position = body != null ? body.get("position") : null;
            Product product = productService.updateFeaturedPosition(id, position);
            String message = Boolean.TRUE.equals(product.getFeatured())
                    ? "Đã cập nhật vị trí nổi bật"
                    : "Đã bỏ sản phẩm khỏi danh sách nổi bật";
            return ResponseEntity.ok(ApiResponse.success(message, product));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating product featured position: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi cập nhật vị trí nổi bật"));
        }
    }

    /**
     * GET /api/admin/products/inventory
     */
    @GetMapping("/inventory")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInventory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {

        try {
            // Validate pagination
            size = Math.min(Math.max(size, 1), 100);
            page = Math.max(page, 0);
            
            Pageable pageable = PageRequest.of(page, size, Sort.by("createDate").descending());
            Page<Product> productPage;

            if (keyword != null && !keyword.isBlank()) {
                productPage = productService.searchProductsPaginated(keyword, pageable);
            } else {
                productPage = productService.findAllPaginated(pageable);
            }

            // Map to simple DTO
            var items = productPage.getContent().stream()
                .map(p -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", p.getId());
                    item.put("name", p.getName());
                    item.put("image", p.getImage());
                    item.put("categoryName", p.getCategory() != null ? p.getCategory().getName() : "");
                    item.put("price", p.getPrice());
                    item.put("available", p.getAvailable());
                    item.put("featured", p.getFeatured());
                    item.put("featuredPosition", p.getFeaturedPosition());
                    return item;
                }).toList();

            Map<String, Object> data = new HashMap<>();
            data.put("items", items);
            data.put("currentPage", productPage.getNumber());
            data.put("totalPages", productPage.getTotalPages());
            data.put("totalItems", productPage.getTotalElements());

            return ResponseEntity.ok(ApiResponse.success("OK", data));
        } catch (Exception e) {
            log.error("Error getting inventory", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi lấy danh sách tồn kho"));
        }
    }

    /**
     * DELETE /api/admin/products/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Integer id) {
        try {
            productService.deleteProductWithValidation(id);
            return ResponseEntity.ok(ApiResponse.success("Xóa sản phẩm thành công", null));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (BusinessRuleException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting product: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi xóa sản phẩm: " + e.getMessage()));
        }
    }

    // ========== PRODUCT IMAGES ENDPOINTS ==========

    /**
     * POST /api/admin/products/{id}/images
     */
    @PostMapping(value = "/{id}/images", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<Void>> uploadProductImages(
            @PathVariable Integer id,
            @RequestParam("images") List<MultipartFile> images) {
        try {
            productValidator.validateImageFiles(images);
            productService.saveProductImages(id, images);
            return ResponseEntity.ok(ApiResponse.success("Upload ảnh thành công", null));
        } catch (BusinessRuleException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error uploading images for product: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi upload ảnh: " + e.getMessage()));
        }
    }

    /**
     * GET /api/admin/products/{id}/images
     */
    @GetMapping("/{id}/images")
    public ResponseEntity<ApiResponse<List<ProductImage>>> getProductImages(@PathVariable Integer id) {
        try {
            List<ProductImage> images = productService.getProductImages(id);
            return ResponseEntity.ok(ApiResponse.success("Lấy danh sách ảnh thành công", images));
        } catch (Exception e) {
            log.error("Error getting images for product: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi lấy danh sách ảnh: " + e.getMessage()));
        }
    }

    /**
     * DELETE /api/admin/products/images/{imageId}
     */
    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteProductImage(@PathVariable Integer imageId) {
        try {
            productService.deleteProductImage(imageId);
            return ResponseEntity.ok(ApiResponse.success("Xóa ảnh thành công", null));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting image: {}", imageId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi xóa ảnh: " + e.getMessage()));
        }
    }
    
    /**
     * POST /api/admin/products/migrate-slugs
     */
    @PostMapping("/migrate-slugs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> migrateProductSlugs() {
        try {
            Map<String, Object> result = productService.generateSlugsForAllProducts();
            return ResponseEntity.ok(ApiResponse.success("Migration hoàn tất", result));
        } catch (Exception e) {
            log.error("Error migrating slugs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi migrate slugs: " + e.getMessage()));
        }
    }
}
