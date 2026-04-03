package com.springboot.jenka_coffee.api.admin;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.service.CategoryService;
import com.springboot.jenka_coffee.service.ProductService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/admin/products")
public class ApiAdminProductController {

    private final ProductService productService;
    private final CategoryService categoryService;

    public ApiAdminProductController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    // GET /api/admin/products?page=0&size=10
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> listProducts(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

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
    public ResponseEntity<ApiResponse<Product>> getProduct(@PathVariable Integer id) {
        Product product = productService.findById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin sản phẩm thành công", product));
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<Product>> createProduct(
            @ModelAttribute Product product,
            @RequestParam("categoryId") String categoryId,
            @RequestParam(value = "imageFile", required = false) MultipartFile file) {
        // Always INSERT — force id=null to prevent accidental update
        product.setId(null);
        Category category = categoryService.findByIdOrThrow(categoryId);
        product.setCategory(category);
        Product saved = productService.saveProduct(product, file);
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

    private ResponseEntity<ApiResponse<Product>> doUpdate(
            Integer id, String name, String description, BigDecimal price,
            String categoryId, Boolean available, MultipartFile file) {
        Product existing = productService.findById(id);
        existing.setName(name);
        existing.setDescription(description);
        existing.setPrice(price);
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

    // PATCH /api/admin/products/{id}/quantity  (cập nhật tồn kho)
    @PatchMapping("/{id}/quantity")
    public ResponseEntity<ApiResponse<Void>> updateQuantity(
            @PathVariable Integer id,
            @RequestBody Map<String, Integer> body) {
        Integer qty = body.get("quantity");
        if (qty == null || qty < 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Số lượng không hợp lệ"));
        }
        // Dùng query trực tiếp — tránh load/save toàn bộ entity
        productService.updateQuantity(id, qty);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật tồn kho thành công", null));
    }

    // GET /api/admin/products/inventory  (danh sách tồn kho)
    @GetMapping("/inventory")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInventory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("quantity").ascending());
        Page<Product> productPage;

        if (keyword != null && !keyword.isBlank()) {
            productPage = productService.searchProductsPaginated(keyword, pageable);
        } else {
            productPage = productService.findAllPaginated(pageable);
        }

        // Filter by stock status nếu có
        var items = productPage.getContent().stream()
            .filter(p -> {
                if ("out".equals(status)) return p.getQuantity() == null || p.getQuantity() == 0;
                if ("low".equals(status)) return p.getQuantity() != null && p.getQuantity() > 0 && p.getQuantity() <= 5;
                return true;
            })
            .map(p -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", p.getId());
                item.put("name", p.getName());
                item.put("image", p.getImage());
                item.put("categoryName", p.getCategory() != null ? p.getCategory().getName() : "");
                item.put("price", p.getPrice());
                item.put("quantity", p.getQuantity() != null ? p.getQuantity() : 0);
                item.put("available", p.getAvailable());
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
            // Dùng phương thức delete() có sẵn trong ProductService (không dùng RedirectAttributes)
            productService.delete(id);
            return ResponseEntity.ok(ApiResponse.success("Xóa sản phẩm thành công", null));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi xóa sản phẩm: " + e.getMessage()));
        }
    }
}
