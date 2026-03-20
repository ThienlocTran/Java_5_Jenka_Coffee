package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.dto.response.StockStatus;
import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.exception.ResourceNotFoundException;
import com.springboot.jenka_coffee.repository.CategoryRepository;
import com.springboot.jenka_coffee.repository.ProductRepository;
import com.springboot.jenka_coffee.service.ProductService;
import com.springboot.jenka_coffee.service.UploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final UploadService uploadService;
    private final CategoryRepository categoryRepository;

    public ProductServiceImpl(ProductRepository productRepository,
            UploadService uploadService,
            CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.uploadService = uploadService;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Override
    @CacheEvict(value = "categoryCounts", allEntries = true)
    public Product saveProduct(Product product, MultipartFile file) {
        log.info("Saving product: {} with image: {}", product.getName(),
                file != null ? file.getOriginalFilename() : "no image");

        // --- XỬ LÝ ẢNH VỚI NÉN ---
        if (file != null && !file.isEmpty()) {
            try {
                // Sử dụng saveProductImage để tự động nén ảnh với preset cho sản phẩm
                String imageUrl = uploadService.saveProductImage(file);
                if (imageUrl != null) {
                    product.setImage(imageUrl);
                    log.info("Successfully uploaded and compressed product image: {}", imageUrl);
                } else {
                    log.warn("Failed to upload product image for: {}", product.getName());
                }
            } catch (Exception e) {
                log.error("Error uploading product image: {}", e.getMessage(), e);
                // Không throw exception, chỉ log lỗi để không block việc lưu product
            }
        }
        // Nếu không có ảnh mới, giữ nguyên ảnh cũ (từ hidden input trong form)

        // --- LƯU VÀO DATABASE ---
        Product savedProduct = productRepository.save(product);
        log.info("Successfully saved product with ID: {}", savedProduct.getId());
        return savedProduct;
    }

    @Override
    @Transactional(readOnly = true)
    public Product findById(Integer id) {
        return productRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    @Override
    public List<Product> findByCategoryId(String cid) {
        // Cách 1: Viết method trong DAO (Khuyên dùng)
        return productRepository.findByCategoryId(cid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getRelatedProducts(String categoryId, Integer productId) {
        return productRepository.findTop4ByCategoryIdAndIdNot(categoryId, productId, PageRequest.of(0, 4));
    }

    @Override
    public List<Product> filterProducts(String categoryId) {
        if (categoryId != null && !categoryId.isEmpty()) {
            return findByCategoryId(categoryId);
        }
        return findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getProductDetail(Integer productId) {
        Product item = findById(productId); // throws ResourceNotFoundException if not found
        String categoryId = item.getCategory() != null ? item.getCategory().getId() : null;
        List<Product> similarItems = categoryId != null
                ? getRelatedProducts(categoryId, item.getId())
                : Collections.emptyList();
        return Map.of("item", item, "similarItems", similarItems);
    }

    @Override
    @CacheEvict(value = "categoryCounts", allEntries = true)
    public Product create(Product product) {
        log.info("Creating new product: {}", product.getName());
        Product savedProduct = productRepository.save(product);
        log.info("Successfully created product with ID: {}", savedProduct.getId());
        return savedProduct;
    }

    @Override
    @CacheEvict(value = "categoryCounts", allEntries = true)
    public Product update(Product product) {
        log.info("Updating product with ID: {}", product.getId());

        // Kiểm tra product có tồn tại không
        if (!productRepository.existsById(product.getId())) {
            throw new ResourceNotFoundException("Product not found with id: " + product.getId());
        }

        Product updatedProduct = productRepository.save(product);
        log.info("Successfully updated product with ID: {}", updatedProduct.getId());
        return updatedProduct;
    }

    @Override
    @CacheEvict(value = "categoryCounts", allEntries = true)
    public void delete(Integer id) {
        log.info("Deleting product with ID: {}", id);

        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }

        productRepository.deleteById(id);
        log.info("Successfully deleted product with ID: {}", id);
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
    @Transactional(readOnly = true)
    public Page<Product> findAllPaginated(Pageable pageable) {
        return productRepository.findAllWithCategory(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> filterProductsPaginated(String categoryId, Pageable pageable) {
        if (categoryId == null || categoryId.trim().isEmpty()) {
            return productRepository.findAll(pageable);
        }
        return productRepository.findByCategoryId(categoryId, pageable);
    }

    @Override
    @Cacheable("categoryCounts")
    public Map<String, Long> getCategoryCounts() {
        Map<String, Long> counts = new HashMap<>();
        List<Object[]> results = productRepository.countProductsGroupedByCategory();
        for (Object[] result : results) {
            String categoryId = (String) result[0];
            Long count = (Long) result[1];
            counts.put(categoryId, count);
        }
        return counts;
    }

    @Override
    public void toggleAvailable(Integer id) {
        log.info("Toggling availability for product ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        boolean newStatus = !product.getAvailable();
        product.setAvailable(newStatus);
        productRepository.save(product);

        log.info("Successfully toggled product ID {} availability to: {}", id, newStatus);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> filterProductsAdvanced(String categoryId, BigDecimal minPrice, BigDecimal maxPrice,
            Pageable pageable) {
        return productRepository.findByCategoryAndPriceRange(categoryId, minPrice, maxPrice, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> searchProductsPaginated(String keyword, Pageable pageable) {
        return productRepository.searchProductsPaginated(keyword, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> filterProductsWithAllCriteria(String categoryId, BigDecimal minPrice, BigDecimal maxPrice,
            String keyword, Pageable pageable) {
        return productRepository.findByAllCriteria(categoryId, minPrice, maxPrice, keyword, pageable);
    }
}
