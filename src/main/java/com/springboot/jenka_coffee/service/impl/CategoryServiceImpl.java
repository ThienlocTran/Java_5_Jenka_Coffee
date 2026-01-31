package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.repository.CategoryRepository; // Nhớ tạo Interface DAO extend JpaRepository nhé
import com.springboot.jenka_coffee.repository.ProductRepository;
import com.springboot.jenka_coffee.service.CategoryService;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Override
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @Override
    public Category findById(String id) {
        return categoryRepository.findById(id).orElse(null);
    }

    @Override
    public Category save(Category category) {
        return categoryRepository.save(category);
    }

    @Override
    public void delete(String id) {
        categoryRepository.deleteById(id);
    }

    @Override
    public boolean existsById(String id) {
        return categoryRepository.existsById(id);
    }

    @Override
    public long countProductsByCategory(String categoryId) {
        return productRepository.countByCategoryId(categoryId);
    }

    @Override
    public Map<String, String> getCategoryIcons() {
        Map<String, String> icons = new java.util.HashMap<>();
        icons.put("CF_AN_VAT", "ca_phe_do_an.webp");
        icons.put("DUNG_CU", "dung_cu_pha_che.webp");
        icons.put("HANG_CU", "may_pha_may_xay_cu.webp");
        icons.put("MAY_PHA", "May_Pha_Ca_Phe.webp");
        icons.put("MAY_XAY", "May_Xay_Ca_Phe.webp");
        icons.put("XAY_EP", "may_xay_sinh_to_may_ep.webp");
        return icons;
    }

    @Override
    public Category findByIdOrThrow(String id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new com.springboot.jenka_coffee.exception.ResourceNotFoundException(
                        "Category", "id", id));
    }

    @Override
    public void deleteOrThrow(String id) {
        // Verify category exists (throws exception if not found)
        findByIdOrThrow(id);

        long productCount = countProductsByCategory(id);
        if (productCount > 0) {
            throw new com.springboot.jenka_coffee.exception.BusinessRuleException(
                    "Không thể xóa loại hàng này vì còn " + productCount + " sản phẩm thuộc loại này!");
        }

        categoryRepository.deleteById(id);
    }

    @Override
    public Category createCategory(com.springboot.jenka_coffee.dto.request.CategoryRequest request) {
        // Check duplicate
        if (categoryRepository.existsById(request.getId())) {
            throw new com.springboot.jenka_coffee.exception.DuplicateResourceException(
                    "Category", "id", request.getId());
        }

        Category category = request.toEntity();
        return categoryRepository.save(category);
    }

    @Override
    public Category updateCategory(String id, com.springboot.jenka_coffee.dto.request.CategoryRequest request) {
        Category existing = findByIdOrThrow(id);

        // Update fields from request
        existing.setName(request.getName());
        if (request.getIcon() != null && !request.getIcon().isEmpty()) {
            existing.setIcon(request.getIcon());
        }

        return categoryRepository.save(existing);
    }
}