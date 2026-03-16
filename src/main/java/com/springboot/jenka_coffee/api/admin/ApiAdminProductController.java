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
        if (product == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Không tìm thấy sản phẩm"));
        }
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin sản phẩm thành công", product));
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<Product>> saveProduct(
            @ModelAttribute Product product,
            @RequestParam("categoryId") String categoryId,
            @RequestParam(value = "imageFile", required = false) MultipartFile file) {
        try {
            // Prevent mass-assignment: force id=null so JPA always INSERT, never UPDATE
            product.setId(null);
            Category category = categoryService.findByIdOrThrow(categoryId);
            product.setCategory(category);
            productService.saveProduct(product, file);
            return ResponseEntity.ok(ApiResponse.success("Lưu sản phẩm thành công", product));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi lưu sản phẩm: " + e.getMessage()));
        }
    }

    // PUT /api/admin/products/{id}/toggle  (bật/tắt trạng thái sản phẩm)
    @PutMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<Void>> toggleAvailable(@PathVariable Integer id) {
        productService.toggleAvailable(id);
        return ResponseEntity.ok(ApiResponse.success("Đổi trạng thái sản phẩm thành công", null));
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
