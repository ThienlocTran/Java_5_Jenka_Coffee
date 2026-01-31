package com.springboot.jenka_coffee.controller.admin;

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
        List<Category> categories = categoryService.findAll();
        Map<String, String> categoryIcons = categoryService.getCategoryIcons();

        model.addAttribute("categories", categories);
        model.addAttribute("categoryIcons", categoryIcons);

        return "admin/categories/category-index";
    }

    /**
     * Hiển thị form thêm loại hàng mới
     */
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("item", new Category());
        return "admin/categories/category-form";
    }

    /**
     * Hiển thị form chỉnh sửa loại hàng
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        Category category = categoryService.findById(id);
        if (category == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy loại hàng!");
            return "redirect:/admin/category/list";
        }
        model.addAttribute("item", category);
        return "admin/categories/category-form";
    }

    /**
     * Lưu loại hàng (thêm mới hoặc cập nhật)
     */
    @PostMapping("/save")
    public String saveCategory(@Valid @ModelAttribute("item") Category category,
            BindingResult result,
            RedirectAttributes redirectAttributes
            ) {

        try {
            // Kiểm tra validation errors
            if (result.hasErrors()) {
                return "admin/categories/category-form";
            }

            // Kiểm tra loại hàng mới
            boolean isNewCategory = (category.getId() == null || category.getId().trim().isEmpty());

            if (isNewCategory) {
                // Kiểm tra ID đã tồn tại
                if (categoryService.existsById(category.getId())) {
                    result.rejectValue("id", "error.category", "Mã loại hàng đã tồn tại!");
                    return "admin/categories/category-form";
                }
            } else {
                // Loại hàng cũ - kiểm tra tồn tại
                Category existingCategory = categoryService.findById(category.getId());
                if (existingCategory == null) {
                    redirectAttributes.addFlashAttribute("error", "Không tìm thấy loại hàng!");
                    return "redirect:/admin/category/list";
                }
            }

            // Chuẩn hóa dữ liệu
            category.setId(category.getId().toUpperCase().trim());
            category.setName(category.getName().trim());

            // Lưu loại hàng
            categoryService.save(category);

            String message = isNewCategory ? "Thêm loại hàng thành công!" : "Cập nhật loại hàng thành công!";
            redirectAttributes.addFlashAttribute("success", message);

            return "redirect:/admin/category/list";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "admin/categories/category-form";
        }
    }

    /**
     * Xóa loại hàng
     */
    @PostMapping("/delete/{id}")
    public String deleteCategory(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            Category category = categoryService.findById(id);
            if (category == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy loại hàng!");
                return "redirect:/admin/category/list";
            }

            // Kiểm tra có sản phẩm thuộc loại này không
            long productCount = categoryService.countProductsByCategory(id);
            if (productCount > 0) {
                redirectAttributes.addFlashAttribute("error",
                        "Không thể xóa loại hàng này vì còn " + productCount + " sản phẩm thuộc loại này!");
                return "redirect:/admin/category/list";
            }

            categoryService.delete(id);
            redirectAttributes.addFlashAttribute("success", "Xóa loại hàng thành công!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không thể xóa loại hàng: " + e.getMessage());
        }

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