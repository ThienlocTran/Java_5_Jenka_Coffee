package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.dto.response.StockStatus;
import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.repository.CategoryRepository;
import com.springboot.jenka_coffee.repository.ProductRepository;
import com.springboot.jenka_coffee.service.ProductService;
import com.springboot.jenka_coffee.service.UploadService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

import java.util.Map;
import java.util.HashMap;

@Service
public class ProductServiceImpl implements ProductService {

    final ProductRepository productRepository;
    final UploadService uploadService;
    final CategoryRepository categoryRepository;

    public ProductServiceImpl(ProductRepository productRepository,
            UploadService uploadService,
            CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.uploadService = uploadService;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Override
    public Product saveProduct(Product product, org.springframework.web.multipart.MultipartFile file) {
        // --- XỬ LÝ ẢNH ---
        // Nếu người dùng có chọn ảnh mới -> Upload lên Cloudinary với nén ảnh
        if (file != null && !file.isEmpty()) {
            String url = uploadService.saveProductImage(file); // Sử dụng method nén ảnh cho sản phẩm
            if (url != null) {
                product.setImage(url);
            }
        }
        // Nếu không chọn ảnh mới -> Giữ nguyên ảnh cũ (Do input hidden trong form lo)

        // --- LƯU VÀO DB ---
        return productRepository.save(product);
    }

    @Override
    public Product findById(Integer id) {
        // .orElse(null) giúp tránh lỗi nếu ID không tồn tại
        return productRepository.findById(id).orElse(null);
    }

    @Override
    public List<Product> findByCategoryId(String cid) {
        // Cách 1: Viết method trong DAO (Khuyên dùng)
        return productRepository.findByCategoryId(cid);
    }

    @Override
    public List<Product> getRelatedProducts(String categoryId, Integer productId) {
        return productRepository.findTop4ByCategoryIdAndIdNot(categoryId, productId);
    }

    @Override
    public List<Product> filterProducts(String categoryId) {
        if (categoryId != null && !categoryId.isEmpty()) {
            return findByCategoryId(categoryId);
        }
        return findAll();
    }

    @Override
    public Map<String, Object> getProductDetail(Integer productId) {
        Product item = findById(productId);
        if (item == null) {
            return null;
        }

        // Handle case where product exists but has no category (though data integrity
        // should prevent this)
        String categoryId = (item.getCategory() != null) ? item.getCategory().getId() : null;
        List<Product> similarItems = (categoryId != null)
                ? getRelatedProducts(categoryId, item.getId())
                : Collections.emptyList();

        Map<String, Object> result = new HashMap<>();
        result.put("item", item);
        result.put("similarItems", similarItems);
        return result;
    }

    @Override
    public Product create(Product product) {
        return productRepository.save(product);
    }

    @Override
    public Product update(Product product) {
        return productRepository.save(product);
    }

    @Override
    public void delete(Integer id) {
        productRepository.deleteById(id);
    }

    @Override
    public StockStatus getStockStatus(Integer quantity) {
        if (quantity == null || quantity == 0) {
            return StockStatus.OUT_OF_STOCK;
        } else if (quantity < 10) {
            return StockStatus.LOW_STOCK;
        } else {
            return StockStatus.IN_STOCK;
        }
    }

    @Override
    public String getStockMessage(Integer quantity) {
        if (quantity == null || quantity == 0) {
            return "Hết hàng";
        } else if (quantity < 10) {
            return "Chỉ còn lại " + quantity + " sản phẩm!";
        } else {
            return "Còn hàng";
        }
    }

    // ========== PAGINATION METHODS ==========

    @Override
    public Page<Product> findAllPaginated(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    @Override
    public Page<Product> filterProductsPaginated(String categoryId, Pageable pageable) {
        if (categoryId == null || categoryId.trim().isEmpty()) {
            return productRepository.findAll(pageable);
        }
        return productRepository.findByCategoryId(categoryId, pageable);
    }

    @Override
    public Map<String, Long> getCategoryCounts() {
        Map<String, Long> counts = new HashMap<>();
        List<Category> categories = categoryRepository.findAll();
        for (Category category : categories) {
            long count = productRepository.countByCategoryId(category.getId());
            counts.put(category.getId(), count);
        }
        return counts;
    }

    @Override
    public void toggleAvailable(Integer id) {
        Product product = productRepository.findById(id).orElse(null);
        if (product != null) {
            product.setAvailable(!product.getAvailable());
            productRepository.save(product);
        }
    }
}