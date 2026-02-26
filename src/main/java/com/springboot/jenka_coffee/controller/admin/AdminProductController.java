package com.springboot.jenka_coffee.controller.admin;

import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.service.CategoryService;
import com.springboot.jenka_coffee.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Controller
@RequestMapping("/admin/product")
public class AdminProductController {

    final ProductService productService;
    final CategoryService categoryService;

    public AdminProductController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    // 1. Admin List View
    @GetMapping("/list")
    public String index(Model model, @RequestParam(value = "p", defaultValue = "0") int page) {
        Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "createDate"));
        Page<Product> productPage = productService.findAllPaginated(pageable);
        model.addAttribute("productPage", productPage);
        model.addAttribute("items", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("totalElements", productPage.getTotalElements());
        return "admin/products/list"; // Admin table view
    }

    // 2. Create Form
    @GetMapping("/create")
    public String create(Model model) {
        Product p = new Product();
        p.setCategory(new Category());
        model.addAttribute("item", p);
        model.addAttribute("categories", categoryService.findAll());
        return "admin/products/form";
    }

    // 3. Edit Form
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Integer id, Model model) {
        Product p = productService.findById(id);
        model.addAttribute("item", p);
        model.addAttribute("categories", categoryService.findAll());
        return "admin/products/form";
    }

    // 4. Save Action
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("item") Product product,
           BindingResult bindingResult,
            @RequestParam(value = "imageFile", required = false) MultipartFile file,
            RedirectAttributes redirectAttributes, Model model) {

        if (bindingResult.hasErrors()) {
            if (product.getCategory() == null) {
                product.setCategory(new Category());
            }
            model.addAttribute("categories", categoryService.findAll());
            return "admin/products/form";
        }

        productService.saveProduct(product, file);
        redirectAttributes.addFlashAttribute("successMessage", "Lưu thành công");
        return "redirect:/admin/product/list";
    }

    // 5. Toggle Available (Soft Delete)
    @GetMapping("/toggle/{id}")
    public String toggleAvailable(@PathVariable("id") Integer id) {
        productService.toggleAvailable(id);
        return "redirect:/admin/product/list";
    }

    // 6. Delete Product (Hard Delete)
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        productService.deleteProduct(id, redirectAttributes);
        return "redirect:/admin/product/list";
    }
}
