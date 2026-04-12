package com.springboot.jenka_coffee.api.admin;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.entity.ProductImage;
import com.springboot.jenka_coffee.service.CategoryService;
import com.springboot.jenka_coffee.service.ProductService;
import com.springboot.jenka_coffee.service.impl.ProductServiceImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/products")
public class ApiAdminProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final com.springboot.jenka_coffee.repository.OrderDetailRepository orderDetailRepository;

    public ApiAdminProductController(ProductService productService, CategoryService categoryService,
                                    com.springboot.jenka_coffee.repository.OrderDetailRepository orderDetailRepository) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.orderDetailRepository = orderDetailRepository;
    }

    // GET /api/admin/products?page=0&size=10
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> listProducts(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        // Giới hạn cứng — tránh OOM
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
    }

    // GET /api/admin/products/{id}
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Product>> getProduct(@PathVariable Integer id) {
        Product product = productService.findById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin sản phẩm thành công", product));
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<Product>> createProduct(
            @ModelAttribute com.springboot.jenka_coffee.dto.request.ProductRequest request,
            @RequestParam("categoryId") String categoryId,
            @RequestParam(value = "imageFile", required = false) MultipartFile file) {
        
        // VULN-MASS-ASSIGNMENT FIX: Dùng DTO thay vì Entity
        // Chỉ map các field được phép từ DTO sang Entity
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice() != null ? 
                request.getPrice().setScale(0, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO);
        product.setAvailable(request.getAvailable() != null ? request.getAvailable() : true);
        product.setRequireContact(request.getRequireContact() != null ? request.getRequireContact() : false);
        
        // Set category
        Category category = categoryService.findByIdOrThrow(categoryId);
        product.setCategory(category);
        
        // Always INSERT — force id=null to prevent accidental update
        product.setId(null);
        
        // Use create() instead of saveProduct() to auto-generate slug
        Product saved = productService.create(product);
        
        // Upload image if provided (after product created)
        if (file != null && !file.isEmpty()) {
            saved = productService.saveProduct(saved, file);
        }
        
        return ResponseEntity.ok(ApiResponse.success("Thêm sản phẩm thành công", saved));
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<Product>> updateProductPut(
            @PathVariable Integer id,
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("price") BigDecimal price,
            @RequestParam("categoryId") String categoryId,
            @RequestParam("available") Boolean available,
            @RequestParam(value = "imageFile", required = false) MultipartFile file) {
        return doUpdate(id, name, description, price, categoryId, available, file);
    }

    @PostMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<Product>> updateProductPost(
            @PathVariable Integer id,
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("price") BigDecimal price,
            @RequestParam("categoryId") String categoryId,
            @RequestParam("available") Boolean available,
            @RequestParam(value = "imageFile", required = false) MultipartFile file) {
        return doUpdate(id, name, description, price, categoryId, available, file);
    }

    @Transactional
    private ResponseEntity<ApiResponse<Product>> doUpdate(
            Integer id, String name, String description, BigDecimal price,
            String categoryId, Boolean available, MultipartFile file) {
        
        // VULN-NEGATIVE-PRICE FIX: Validate price is not negative
        if (price != null && price.compareTo(BigDecimal.ZERO) < 0) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Giá sản phẩm không thể âm!"));
        }
        
        Product existing = productService.findById(id);
        existing.setName(name);
        existing.setDescription(description);
        existing.setPrice(price != null ? price.setScale(0, java.math.RoundingMode.HALF_UP) : price);
        existing.setAvailable(available);
        existing.setCategory(categoryService.findByIdOrThrow(categoryId));
        Product saved = productService.saveProduct(existing, file);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật sản phẩm thành công", saved));
    }

    // PUT /api/admin/products/{id}/toggle  (bật/tắt trạng thái sản phẩm)
    @PutMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<Void>> toggleAvailable(@PathVariable Integer id) {
        productService.toggleAvailable(id);
        return ResponseEntity.ok(ApiResponse.success("Đổi trạng thái sản phẩm thành công", null));
    }

    // PUT /api/admin/products/{id}/toggle-featured  (đánh dấu sản phẩm nổi bật)
    @PutMapping("/{id}/toggle-featured")
    public ResponseEntity<ApiResponse<Product>> toggleFeatured(@PathVariable Integer id) {
        Product product = productService.findById(id);
        if (product == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Không tìm thấy sản phẩm"));
        }
        
        // Toggle featured status
        product.setFeatured(!Boolean.TRUE.equals(product.getFeatured()));
        Product saved = ((ProductServiceImpl) productService).update(product);
        
        String message = Boolean.TRUE.equals(saved.getFeatured()) 
            ? "Đã đánh dấu sản phẩm nổi bật" 
            : "Đã bỏ đánh dấu sản phẩm nổi bật";
        return ResponseEntity.ok(ApiResponse.success(message, saved));
    }

    // GET /api/admin/products/inventory  (danh sách sản phẩm - không quản lý tồn kho)
    @GetMapping("/inventory")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInventory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createDate").descending());
        Page<Product> productPage;

        if (keyword != null && !keyword.isBlank()) {
            productPage = productService.searchProductsPaginated(keyword, pageable);
        } else {
            productPage = productService.findAllPaginated(pageable);
        }

        var items = productPage.getContent().stream()
            .map(p -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", p.getId());
                item.put("name", p.getName());
                item.put("image", p.getImage());
                item.put("categoryName", p.getCategory() != null ? p.getCategory().getName() : "");
                item.put("price", p.getPrice());
                item.put("available", p.getAvailable());
                item.put("featured", p.getFeatured()); // Add featured status
                return item;
            }).toList();

        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        data.put("currentPage", productPage.getNumber());
        data.put("totalPages", productPage.getTotalPages());
        data.put("totalItems", productPage.getTotalElements());

        return ResponseEntity.ok(ApiResponse.success("OK", data));
    }

    // DELETE /api/admin/products/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Integer id) {
        try {
            // VULN-CONSTRAINT-VIOLATION FIX: Check if product is used in orders before deletion
            long orderCount = orderDetailRepository.countByProductId(id);
            if (orderCount > 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error(
                                "Không thể xóa sản phẩm này vì đã có " + orderCount + 
                                " đơn hàng sử dụng. Bạn có thể ẩn sản phẩm thay vì xóa."));
            }
            
            productService.delete(id);
            return ResponseEntity.ok(ApiResponse.success("Xóa sản phẩm thành công", null));
        } catch (com.springboot.jenka_coffee.exception.ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Không tìm thấy sản phẩm"));
        } catch (com.springboot.jenka_coffee.exception.BusinessRuleException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi xóa sản phẩm: " + e.getMessage()));
        }
    }

    // ========== PRODUCT IMAGES ENDPOINTS ==========

    // POST /api/admin/products/{id}/images - Upload nhiều ảnh cho sản phẩm
    @PostMapping(value = "/{id}/images", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<Void>> uploadProductImages(
            @PathVariable Integer id,
            @RequestParam("images") List<MultipartFile> images) {
        try {
            if (images == null || images.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Vui lòng chọn ít nhất 1 ảnh"));
            }
            ((ProductServiceImpl) productService).saveProductImages(id, images);
            return ResponseEntity.ok(ApiResponse.success("Upload ảnh thành công", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi upload ảnh: " + e.getMessage()));
        }
    }

    // GET /api/admin/products/{id}/images - Lấy danh sách ảnh của sản phẩm
    @GetMapping("/{id}/images")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<ProductImage>>> getProductImages(@PathVariable Integer id) {
        try {
            List<ProductImage> images = ((ProductServiceImpl) productService).getProductImages(id);
            return ResponseEntity.ok(ApiResponse.success("Lấy danh sách ảnh thành công", images));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi lấy danh sách ảnh: " + e.getMessage()));
        }
    }

    // DELETE /api/admin/products/images/{imageId} - Xóa 1 ảnh
    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteProductImage(@PathVariable Integer imageId) {
        try {
            ((ProductServiceImpl) productService).deleteProductImage(imageId);
            return ResponseEntity.ok(ApiResponse.success("Xóa ảnh thành công", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi xóa ảnh: " + e.getMessage()));
        }
    }
    
    // POST /api/admin/products/migrate-slugs - Generate slug cho tất cả sản phẩm
    @PostMapping("/migrate-slugs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> migrateProductSlugs() {
        try {
            Map<String, Object> result = ((ProductServiceImpl) productService).generateSlugsForAllProducts();
            return ResponseEntity.ok(ApiResponse.success("Migration hoàn tất", result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi migrate slugs: " + e.getMessage()));
        }
    }
}
