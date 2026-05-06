package com.springboot.jenka_coffee.api.admin;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.dto.request.CategoryRequest;
import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.exception.BusinessRuleException;
import com.springboot.jenka_coffee.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/categories")
public class ApiAdminCategoryController {

    private final CategoryService categoryService;

    public ApiAdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> listCategories(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        // VULN #20 FIX: Prevent deep pagination DoS
        // PROBLEM: No limit on size parameter → attacker can request size=Integer.MAX_VALUE
        // - JVM tries to allocate huge list → OutOfMemoryError
        // - Only requires admin account or insider access
        // SOLUTION: Enforce reasonable limits
        // - Min size: 1 (prevent zero/negative)
        // - Max size: 100 (reasonable for admin pagination)
        // - Min page: 0 (prevent negative)
        size = Math.min(Math.max(size, 1), 100);
        page = Math.max(page, 0);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Category> categoryPage = categoryService.findAllPaginated(pageable);

        Map<String, Object> data = new HashMap<>();
        data.put("items", categoryPage.getContent());
        data.put("currentPage", categoryPage.getNumber());
        data.put("totalPages", categoryPage.getTotalPages());
        data.put("totalItems", categoryPage.getTotalElements());

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách danh mục thành công", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Category>> getCategory(@PathVariable String id) {
        Category category = categoryService.findByIdOrThrow(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin danh mục thành công", category));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Category>> createCategory(@Valid @RequestBody CategoryRequest request) {
        // FIX: Manual validation for id since @NotBlank was removed from DTO
        if (request.getId() == null || request.getId().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("ID danh mục không được để trống"));
        }
        
        if (categoryService.existsById(request.getId().trim())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("ID danh mục đã tồn tại"));
        }
        Category category = categoryService.createCategory(request);
        return ResponseEntity.ok(ApiResponse.success("Thêm mới danh mục thành công", category));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Category>> updateCategory(
            @PathVariable String id,
            @Valid @RequestBody CategoryRequest request) {
        Category category = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật danh mục thành công", category));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable String id) {
        // FIX: Removed catch-all Exception handler that was returning 500 for ResourceNotFoundException
        // Let GlobalExceptionHandler handle all exceptions consistently:
        // - ResourceNotFoundException → 404
        // - BusinessRuleException → 400
        // - Other exceptions → 500
        categoryService.deleteOrThrow(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa danh mục thành công", null));
    }

    @GetMapping("/check-id/{id}")
    public ResponseEntity<ApiResponse<Boolean>> checkCategoryId(@PathVariable String id) {
        boolean isAvailable = !categoryService.existsById(id.toUpperCase().trim());
        return ResponseEntity.ok(ApiResponse.success("Kiểm tra ID thành công", isAvailable));
    }

    @GetMapping("/product-count/{id}")
    public ResponseEntity<ApiResponse<Long>> getProductCount(@PathVariable String id) {
        long count = categoryService.countProductsByCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy số lượng sản phẩm thành công", count));
    }
}
