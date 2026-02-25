package com.springboot.jenka_coffee.controller.admin;

import com.springboot.jenka_coffee.dto.request.CategoryRequest;
import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.service.CategoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Controller
@RequestMapping("/admin/category")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Hiển thị danh sách loại hàng
     */
    @GetMapping("/list")
    public String listCategories(Model model) {
        // Initial load: 10 items
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));
        Page<Category> categoryPage = categoryService.findAllPaginated(pageable);
        model.addAttribute("categories", categoryPage.getContent());
        model.addAttribute("totalElements", categoryPage.getTotalElements());
        return "admin/categories/category-index";
    }

    /**
     * Lazy load HTML fragment
     */
    @GetMapping("/fragment/list")
    public String loadMoreCategories(@RequestParam(value = "p", defaultValue = "2") int page, Model model) {
        // Offset logic: initial load 10 items. Our fragment size is 5.
        // Therefore, page * size => 2 * 5 = 10 (items 10-14).
        Pageable pageable = PageRequest.of(page, 5, Sort.by(Sort.Direction.DESC, "id"));
        Page<Category> categoryPage = categoryService.findAllPaginated(pageable);
        model.addAttribute("categories", categoryPage.getContent());
        return "admin/categories/category-index :: categoryRows";
    }

    /**
     * Hiển thị form thêm loại hàng mới
     */
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("item", new CategoryRequest());
        return "admin/categories/category-form";
    }

    /**
     * Hiển thị form chỉnh sửa loại hàng
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id, Model model) {
        Category category = categoryService.findByIdOrThrow(id);
        model.addAttribute("item", CategoryRequest.fromEntity(category));
        return "admin/categories/category-form";
    }

    /**
     * Lưu loại hàng (thêm mới hoặc cập nhật)
     */
    @PostMapping("/save")
    public String saveCategory(
            @Valid @ModelAttribute("item") CategoryRequest request,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        // Check validation errors from @Valid
        if (result.hasErrors()) {
            return "admin/categories/category-form";
        }

        boolean isNewCategory = (request.getId() == null || request.getId().trim().isEmpty());

        // Delegate to service - exceptions handled by GlobalExceptionHandler
        if (isNewCategory) {
            categoryService.createCategory(request);
        } else {
            categoryService.updateCategory(request.getId(), request);
        }

        String message = isNewCategory ? "Thêm loại hàng thành công!" : "Cập nhật loại hàng thành công!";
        redirectAttributes.addFlashAttribute("success", message);

        return "redirect:/admin/category/list";
    }

    /**
     * Xóa loại hàng
     */
    @PostMapping("/delete/{id}")
    public String deleteCategory(@PathVariable String id, RedirectAttributes redirectAttributes) {
        // Service throws BusinessRuleException if category has products
        categoryService.deleteOrThrow(id);
        redirectAttributes.addFlashAttribute("success", "Xóa loại hàng thành công!");
        return "redirect:/admin/category/list";
    }

    /**
     * API endpoint để kiểm tra ID loại hàng có tồn tại không (AJAX)
     */
    @GetMapping("/check-id")
    @ResponseBody
    public boolean checkCategoryId(@RequestParam String id) {
        return !categoryService.existsById(id.toUpperCase().trim());
    }

    /**
     * API endpoint để lấy số lượng sản phẩm theo loại (AJAX)
     */
    @GetMapping("/product-count/{id}")
    @ResponseBody
    public long getProductCount(@PathVariable String id) {
        return categoryService.countProductsByCategory(id);
    }
}
