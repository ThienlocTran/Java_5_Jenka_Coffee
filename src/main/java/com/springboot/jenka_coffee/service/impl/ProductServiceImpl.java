package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.entity.ProductImage;
import com.springboot.jenka_coffee.exception.ResourceNotFoundException;
import com.springboot.jenka_coffee.repository.CategoryRepository;
import com.springboot.jenka_coffee.repository.ProductRepository;
import com.springboot.jenka_coffee.repository.ProductImageRepository;
import com.springboot.jenka_coffee.service.ProductService;
import com.springboot.jenka_coffee.service.UploadService;
import com.springboot.jenka_coffee.util.SlugUtils;
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
    private final ProductImageRepository productImageRepository;

    public ProductServiceImpl(ProductRepository productRepository,
            UploadService uploadService,
            CategoryRepository categoryRepository,
            ProductImageRepository productImageRepository) {
        this.productRepository = productRepository;
        this.uploadService = uploadService;
        this.categoryRepository = categoryRepository;
        this.productImageRepository = productImageRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Override
    @Transactional
    @CacheEvict(value = "categoryCounts", allEntries = true)
    public Product saveProduct(Product product, MultipartFile file) {
        log.info("Saving product: {} with image: {}", product.getName(),
                file != null ? file.getOriginalFilename() : "no image");

        if (file != null && !file.isEmpty()) {
            try {
                String imageUrl = uploadService.saveProductImage(file);
                if (imageUrl != null) {
                    product.setImage(imageUrl);
                    log.info("Successfully uploaded and compressed product image: {}", imageUrl);
                } else {
                    log.warn("Failed to upload product image for: {}", product.getName());
                }
            } catch (Exception e) {
                log.error("Error uploading product image: {}", e.getMessage(), e);
            }
        }

        Product savedProduct = productRepository.save(product);
        log.info("Successfully saved product with ID: {}", savedProduct.getId());
        // Reload với JOIN FETCH để category không còn là lazy proxy
        return productRepository.findByIdWithCategory(savedProduct.getId())
                .orElse(savedProduct);
    }

    @Transactional
    public void saveProductImages(Integer productId, List<MultipartFile> imageFiles) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        int order = 0;
        for (MultipartFile file : imageFiles) {
            if (file != null && !file.isEmpty()) {
                try {
                    String imageUrl = uploadService.saveProductImage(file);
                    if (imageUrl != null) {
                        ProductImage productImage = new ProductImage();
                        productImage.setProduct(product);
                        productImage.setImageUrl(imageUrl);
                        productImage.setDisplayOrder(order++);
                        productImage.setIsPrimary(order == 1); // First image is primary
                        productImageRepository.save(productImage);
                        log.info("Saved product image: {} for product ID: {}", imageUrl, productId);
                    }
                } catch (Exception e) {
                    log.error("Error uploading product image: {}", e.getMessage(), e);
                }
            }
        }
    }

    @Transactional
    public void deleteProductImage(Integer imageId) {
        productImageRepository.deleteById(imageId);
        log.info("Deleted product image with ID: {}", imageId);
    }

    @Transactional(readOnly = true)
    public List<ProductImage> getProductImages(Integer productId) {
        return productImageRepository.findByProductIdOrderByDisplayOrderAscIdAsc(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public Product findById(Integer id) {
        return productRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getRelatedProducts(String categoryId, Integer productId) {
        return productRepository.findTop4ByCategoryIdAndIdNot(categoryId, productId, PageRequest.of(0, 4));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getProductDetail(Integer productId) {
        // Không cần fetch images ở đây - frontend sẽ gọi API /products/{id}/images riêng
        Product item = findById(productId);
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
        
        // Generate slug from product name
        product.setSlug(generateUniqueSlug(product.getName()));
        
        Product savedProduct = productRepository.save(product);
        log.info("Successfully created product with ID: {} and slug: {}", savedProduct.getId(), savedProduct.getSlug());
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
        
        // Regenerate slug if name changed
        Product existingProduct = productRepository.findById(product.getId()).orElseThrow();
        if (!existingProduct.getName().equals(product.getName())) {
            product.setSlug(generateUniqueSlug(product.getName()));
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
    @Transactional
    public void updateQuantity(Integer id, Integer quantity) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productRepository.updateQuantityById(id, quantity);
        log.info("Updated quantity for product ID {} to {}", id, quantity);
    }

    // ========== PAGINATION METHODS ==========

    @Override
    @Transactional(readOnly = true)
    public Page<Product> findAllPaginated(Pageable pageable) {
        return productRepository.findAllWithCategory(pageable);
    }

    @Override
    @Cacheable("categoryCounts")
    public Map<String, Long> getCategoryCounts() {
        Map<String, Long> counts = new HashMap<>();
        List<Object[]> results = productRepository.countProductsGroupedByCategory();
        long total = 0L;
        for (Object[] result : results) {
            String categoryId = (String) result[0];
            Long count = (Long) result[1];
            counts.put(categoryId, count);
            total += count;
        }
        counts.put("ALL", total);
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
    public Page<Product> filterProductsWithAllCriteria(String categoryId, BigDecimal minPrice, BigDecimal maxPrice,
            String keyword, Pageable pageable) {
        return productRepository.findByAllCriteria(categoryId, minPrice, maxPrice, keyword, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> searchProductsPaginated(String keyword, Pageable pageable) {
        return productRepository.searchProductsPaginated(keyword, pageable);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Product findBySlug(String slug) {
        log.info("Finding product by slug: {}", slug);
        return productRepository.findBySlugWithCategory(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with slug: " + slug));
    }
    
    /**
     * Generate unique slug from product name
     */
    private String generateUniqueSlug(String productName) {
        String baseSlug = SlugUtils.toSlug(productName);
        String slug = baseSlug;
        int counter = 0;
        
        // Check if slug exists, if yes, append number
        while (productRepository.existsBySlug(slug)) {
            counter++;
            slug = baseSlug + "-" + counter;
        }
        
        return slug;
    }
    
    /**
     * Generate slugs for all existing products (Migration)
     */
    @Transactional
    public Map<String, Object> generateSlugsForAllProducts() {
        log.info("Starting slug migration for all products");
        
        List<Product> allProducts = productRepository.findAll();
        int totalProducts = allProducts.size();
        int updatedCount = 0;
        int skippedCount = 0;
        int errorCount = 0;
        
        for (Product product : allProducts) {
            try {
                // Skip if already has slug
                if (product.getSlug() != null && !product.getSlug().isEmpty()) {
                    skippedCount++;
                    continue;
                }
                
                // Generate and save slug
                String slug = generateUniqueSlug(product.getName());
                product.setSlug(slug);
                productRepository.save(product);
                updatedCount++;
                
                log.info("Generated slug '{}' for product ID: {}", slug, product.getId());
            } catch (Exception e) {
                errorCount++;
                log.error("Failed to generate slug for product ID: {} - {}", product.getId(), e.getMessage());
            }
        }
        
        log.info("Slug migration completed. Total: {}, Updated: {}, Skipped: {}, Errors: {}", 
                 totalProducts, updatedCount, skippedCount, errorCount);
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalProducts", totalProducts);
        result.put("updatedCount", updatedCount);
        result.put("skippedCount", skippedCount);
        result.put("errorCount", errorCount);
        
        return result;
    }
}
