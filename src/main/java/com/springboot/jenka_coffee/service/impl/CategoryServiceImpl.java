package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.dto.request.CategoryRequest;
import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.exception.BusinessRuleException;
import com.springboot.jenka_coffee.exception.DuplicateResourceException;
import com.springboot.jenka_coffee.exception.ResourceNotFoundException;
import com.springboot.jenka_coffee.repository.CategoryRepository;
import com.springboot.jenka_coffee.repository.ProductRepository;
import com.springboot.jenka_coffee.service.CategoryService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> findAll() {
        List<Category> categories = categoryRepository.findAll();
        // Initialize products collection để tránh lazy loading exception
        categories.forEach(cat -> {
            if (cat.getProducts() != null) {
                cat.getProducts().size(); // Trigger lazy load
            }
        });
        return categories;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Category> findAllPaginated(Pageable pageable) {
        Page<Category> categoryPage = categoryRepository.findAll(pageable);
        categoryPage.getContent().forEach(cat -> {
            if (cat.getProducts() != null) {
                cat.getProducts().size();
            }
        });
        return categoryPage;
    }

    @Override
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public boolean existsById(String id) {
        return categoryRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long countProductsByCategory(String categoryId) {
        // Đơn giản hóa - trả về 0 nếu ProductRepository chưa có method này
        try {
            return productRepository.countByCategoryId(categoryId);
        } catch (Exception e) {
            // Log warning và trả về 0 để không crash app
            System.out.println("WARNING: ProductRepository.countByCategoryId() not implemented yet. Returning 0.");
            return 0;
        }
    }

    @Override
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public Category findByIdOrThrow(String id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category", "id", id));
    }

    @Override
    public void deleteOrThrow(String id) {
        // Verify category exists (throws exception if not found)
        findByIdOrThrow(id);

        long productCount = countProductsByCategory(id);
        if (productCount > 0) {
            throw new BusinessRuleException(
                    "Không thể xóa loại hàng này vì còn " + productCount + " sản phẩm thuộc loại này!");
        }

        categoryRepository.deleteById(id);
    }

    @Override
    public Category createCategory(CategoryRequest request) {
        // Normalize data
        request.normalize();

        // Check duplicate
        if (categoryRepository.existsById(request.getId())) {
            throw new DuplicateResourceException(
                    "Category", "id", request.getId());
        }

        Category category = request.toEntity();

        // Set icon from predefined list if not provided
        if (category.getIcon() == null || category.getIcon().isEmpty()) {
            Map<String, String> icons = getCategoryIcons();
            category.setIcon(icons.get(category.getId()));
        }

        return categoryRepository.save(category);
    }

    @Override
    public Category updateCategory(String id, CategoryRequest request) {
        Category existing = findByIdOrThrow(id);

        // Normalize data
        request.normalize();

        // Update fields from request
        existing.setName(request.getName());

        // Update icon if provided, otherwise keep existing or set from predefined list
        if (request.getIcon() != null && !request.getIcon().isEmpty()) {
            existing.setIcon(request.getIcon());
        } else if (existing.getIcon() == null || existing.getIcon().isEmpty()) {
            Map<String, String> icons = getCategoryIcons();
            existing.setIcon(icons.get(existing.getId()));
        }

        return categoryRepository.save(existing);
    }
}