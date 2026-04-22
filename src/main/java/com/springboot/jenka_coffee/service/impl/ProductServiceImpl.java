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
import com.springboot.jenka_coffee.service.VercelWebhookService;
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
    private final VercelWebhookService vercelWebhookService;

    public ProductServiceImpl(ProductRepository productRepository,
            UploadService uploadService,
            CategoryRepository categoryRepository,
            ProductImageRepository productImageRepository,
            VercelWebhookService vercelWebhookService) {
        this.productRepository = productRepository;
        this.uploadService = uploadService;
        this.categoryRepository = categoryRepository;
        this.productImageRepository = productImageRepository;
        this.vercelWebhookService = vercelWebhookService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    // 🚨 BUG-62: NETWORK I/O INSIDE DATABASE TRANSACTION (Tắc Nghẽn Động Mạch Mạng)
    // ================================================================
    // CRITICAL PERFORMANCE BOTTLENECK: Cloudinary upload blocks database connection!
    // 
    // Current state:
    // - Method annotated with @Transactional
    // - Inside transaction: uploadService.saveProductImage(file) uploads to Cloudinary
    // - Upload takes 1-3 seconds over network
    // - Database connection held for entire duration
    // 
    // The problem:
    // When admin uploads product image:
    // 1. Spring opens database connection from HikariCP pool (default: 10 connections)
    // 2. Transaction begins
    // 3. uploadService.saveProductImage() uploads 5MB file to Cloudinary over internet
    // 4. Network I/O takes 1-3 seconds (slow!)
    // 5. Database connection BLOCKED for entire 3 seconds (doing nothing!)
    // 6. Connection pool exhausted if 10 admins upload simultaneously
    // 7. Request #11 gets "Connection timeout" error
    // 8. Application crashes or becomes unresponsive
    // 
    // This is called "Connection Pool Starvation" - a classic performance anti-pattern.
    // 
    // Scenario timeline:
    // ```
    // Time  | Action                           | DB Connections Used
    // ------|----------------------------------|--------------------
    // 0.0s  | Admin 1 uploads image            | 1/10
    // 0.1s  | Admin 2 uploads image            | 2/10
    // 0.2s  | Admin 3 uploads image            | 3/10
    // ...   | ...                              | ...
    // 1.0s  | Admin 10 uploads image           | 10/10 (POOL FULL!)
    // 1.1s  | Customer tries to checkout       | BLOCKED! (timeout)
    // 1.2s  | Customer tries to view products  | BLOCKED! (timeout)
    // 1.3s  | Admin 11 tries to upload         | BLOCKED! (timeout)
    // 3.0s  | First upload completes           | 9/10 (1 freed)
    // ```
    // 
    // Business impact:
    // - Website becomes unresponsive during image uploads
    // - Customers cannot checkout (lost sales!)
    // - Admins cannot manage products
    // - Database connections wasted on network I/O
    // - Cannot scale beyond 10 concurrent uploads
    // - Poor user experience (timeouts, errors)
    // 
    // Root cause:
    // NEVER perform network I/O inside database transactions!
    // - HTTP calls (external APIs, webhooks)
    // - File uploads (Cloudinary, S3, Azure Blob)
    // - Email sending (SMTP)
    // - Message queue publishing (RabbitMQ, Kafka)
    // 
    // Solution (NEEDS TO BE REFACTORED):
    // 
    // 1. Split into two methods - upload OUTSIDE transaction:
    // ```java
    // // Public method (NO @Transactional) - orchestrates workflow
    // public Product saveProduct(Product product, MultipartFile file) {
    //     log.info("Saving product: {}", product.getName());
    //     
    //     // STEP 1: Upload image OUTSIDE transaction (can take 3 seconds)
    //     String imageUrl = null;
    //     String oldImageUrl = null;
    //     
    //     if (product.getId() != null) {
    //         Product existing = productRepository.findById(product.getId()).orElse(null);
    //         if (existing != null) {
    //             oldImageUrl = existing.getImage();
    //         }
    //     }
    //     
    //     if (file != null && !file.isEmpty()) {
    //         imageUrl = uploadService.saveProductImage(file); // 3 seconds - NO DB connection held!
    //         if (imageUrl != null) {
    //             product.setImage(imageUrl);
    //         }
    //     }
    //     
    //     // STEP 2: Save to database INSIDE transaction (takes 10ms)
    //     Product savedProduct = saveProductTransactional(product);
    //     
    //     // STEP 3: Cleanup old image OUTSIDE transaction
    //     if (oldImageUrl != null && imageUrl != null && !oldImageUrl.equals(imageUrl)) {
    //         try {
    //             uploadService.deleteImage(oldImageUrl);
    //         } catch (Exception e) {
    //             log.warn("Failed to delete old image: {}", e.getMessage());
    //         }
    //     }
    //     
    //     return savedProduct;
    // }
    // 
    // // Private method WITH @Transactional - only database operations
    // @Transactional
    // private Product saveProductTransactional(Product product) {
    //     Product saved = productRepository.save(product);
    //     return productRepository.findByIdWithCategory(saved.getId()).orElse(saved);
    // }
    // ```
    // 
    // 2. Alternative: Use @Async for upload (non-blocking):
    // ```java
    // @Transactional
    // public Product saveProduct(Product product, MultipartFile file) {
    //     // Save product first with temporary image URL
    //     product.setImage("pending-upload");
    //     Product saved = productRepository.save(product);
    //     
    //     // Upload asynchronously (doesn't block transaction)
    //     if (file != null && !file.isEmpty()) {
    //         uploadService.saveProductImageAsync(saved.getId(), file);
    //     }
    //     
    //     return saved;
    // }
    // 
    // @Async
    // @Transactional
    // public void saveProductImageAsync(Integer productId, MultipartFile file) {
    //     String imageUrl = uploadService.saveProductImage(file); // Runs in separate thread
    //     Product product = productRepository.findById(productId).orElseThrow();
    //     product.setImage(imageUrl);
    //     productRepository.save(product);
    // }
    // ```
    // 
    // Performance comparison:
    // BEFORE (current):
    // - Transaction duration: 3,000ms (3 seconds)
    // - DB connection held: 3,000ms
    // - Max concurrent uploads: 10 (connection pool size)
    // - Throughput: 3.3 uploads/second (10 connections / 3 seconds)
    // 
    // AFTER (refactored):
    // - Transaction duration: 10ms (database save only)
    // - DB connection held: 10ms
    // - Max concurrent uploads: 1,000+ (limited by CPU/memory, not DB)
    // - Throughput: 1,000 uploads/second (10 connections / 0.01 seconds)
    // 
    // 300x performance improvement!
    // 
    // Other places with same issue:
    // - AccountServiceImpl.createAccount() - uploads avatar inside transaction
    // - EmailServiceImpl - sends email inside transaction (if @Transactional)
    // - Any service that calls external APIs inside @Transactional methods
    // 
    // Best practices:
    // 1. Keep transactions SHORT (< 100ms)
    // 2. Only database operations inside transactions
    // 3. Network I/O OUTSIDE transactions
    // 4. Use @Async for long-running operations
    // 5. Monitor transaction duration (should be < 100ms)
    // 6. Use connection pool monitoring (HikariCP metrics)
    // 
    // Related patterns:
    // - Saga pattern (distributed transactions)
    // - Outbox pattern (reliable message publishing)
    // - Two-phase commit (distributed systems)
    // 
    // Monitoring:
    // ```java
    // // Add to application.properties
    // spring.datasource.hikari.maximum-pool-size=10
    // spring.datasource.hikari.leak-detection-threshold=60000
    // management.metrics.enable.hikari=true
    // 
    // // Monitor metrics
    // hikaricp.connections.active (should be < 8)
    // hikaricp.connections.pending (should be 0)
    // hikaricp.connections.timeout.total (should be 0)
    // ```
    // ================================================================

    @Override
    @CacheEvict(value = "categoryCounts", allEntries = true)
    public Product saveProduct(Product product, MultipartFile file) {
        log.info("Saving product: {} with image: {}", product.getName(),
                file != null ? file.getOriginalFilename() : "no image");

        // VULN-ORPHANED-STORAGE FIX: Delete old image if updating existing product
        String oldImageUrl = null;
        if (product.getId() != null) {
            Product existingProduct = productRepository.findById(product.getId()).orElse(null);
            if (existingProduct != null) {
                oldImageUrl = existingProduct.getImage();
            }
        }

        // 1. GỌI MẠNG CLOUDINARY BÊN NGOÀI @Transactional
        if (file != null && !file.isEmpty()) {
            try {
                String imageUrl = uploadService.saveProductImage(file);
                if (imageUrl != null) {
                    product.setImage(imageUrl);
                    log.info("Successfully uploaded and compressed product image: {}", imageUrl);
                    
                    // Delete old image after successful upload
                    if (oldImageUrl != null && !oldImageUrl.isEmpty() && !oldImageUrl.equals(imageUrl)) {
                        try {
                            uploadService.deleteImage(oldImageUrl);
                        } catch (Exception e) {
                            log.warn("Failed to delete old product image: {}", e.getMessage());
                        }
                    }
                } else {
                    log.warn("Failed to upload product image for: {}", product.getName());
                }
            } catch (Exception e) {
                log.error("Error uploading product image: {}", e.getMessage(), e);
            }
        }

        // 2. LƯU BÀO DATABASE MỚI MỞ TRANSACTION
        return saveProductTransactional(product);
    }
    
    @Transactional
    public Product saveProductTransactional(Product product) {
        Product savedProduct = productRepository.save(product);
        log.info("Successfully saved product with ID: {}", savedProduct.getId());
        
        // Trigger Vercel rebuild after successful save
        vercelWebhookService.triggerRebuild();
        
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
        // VULN-ORPHANED-CLOUDINARY-STORAGE FIX: Delete from Cloudinary before deleting DB record
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ảnh với ID: " + imageId));
        
        // Delete from Cloudinary first
        if (image.getImageUrl() != null && !image.getImageUrl().isEmpty()) {
            try {
                uploadService.deleteImage(image.getImageUrl());
            } catch (Exception e) {
                log.warn("Failed to delete image from Cloudinary: {}", e.getMessage());
                // Continue with DB deletion even if Cloudinary deletion fails
                // Better to have orphaned cloud storage than broken DB references
            }
        }
        
        // Delete from database
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
        
        // BUG-45 FIX: Retry logic for distributed race condition on slug
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                // Generate slug from product name
                product.setSlug(generateUniqueSlug(product.getName()));
                
                Product savedProduct = productRepository.save(product);
                log.info("Successfully created product with ID: {} and slug: {}", savedProduct.getId(), savedProduct.getSlug());
                return savedProduct;
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                // Slug collision detected (race condition in distributed system)
                if (attempt < maxAttempts) {
                    log.warn("Slug collision detected on attempt {}, retrying with new slug", attempt);
                    // Add small random delay to reduce collision probability
                    try {
                        Thread.sleep(50 + (long)(Math.random() * 100));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    log.error("Failed to create product after {} attempts due to slug collision", maxAttempts);
                    throw new RuntimeException("Không thể tạo sản phẩm sau nhiều lần thử. Vui lòng thử lại.", e);
                }
            }
        }
        
        throw new RuntimeException("Không thể tạo sản phẩm");
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
        
        // Trigger Vercel rebuild after successful update
        vercelWebhookService.triggerRebuild();
        
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
        
        // Trigger Vercel rebuild after successful delete
        vercelWebhookService.triggerRebuild();
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
     * BUG-45 FIX: Generate unique slug with retry logic for distributed systems
     * 
     * PROBLEM: synchronized keyword only works within single JVM instance
     * In load-balanced/multi-server deployments, two servers can generate same slug simultaneously
     * causing DataIntegrityViolationException when both try to insert
     * 
     * SOLUTION: Use optimistic retry approach - catch duplicate key exception and retry with new slug
     * This works across distributed systems without requiring Redis/distributed locks
     * 
     * For production with high concurrency, consider:
     * 1. Redis distributed lock (Redisson)
     * 2. Database-level unique constraint (already exists)
     * 3. This retry mechanism (current implementation - good enough for most cases)
     */
    private String generateUniqueSlug(String productName) {
        String baseSlug = SlugUtils.toSlug(productName);
        String slug = baseSlug;
        int counter = 0;
        int maxRetries = 10;
        
        // Try to find unique slug, with retry limit to prevent infinite loop
        while (counter < maxRetries && productRepository.existsBySlug(slug)) {
            counter++;
            slug = baseSlug + "-" + counter;
        }
        
        if (counter >= maxRetries) {
            // Fallback: append timestamp to guarantee uniqueness
            slug = baseSlug + "-" + System.currentTimeMillis();
            log.warn("Slug generation exceeded max retries, using timestamp: {}", slug);
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
    
    // ========== ADMIN OPERATIONS WITH BUSINESS LOGIC ==========
    
    /**
     * Create product from request DTO with validation
     */
    @Override
    @Transactional
    public Product createProductFromRequest(com.springboot.jenka_coffee.dto.request.ProductRequest request, 
                                           String categoryId, MultipartFile imageFile) {
        log.info("Creating product from request: {}", request.getName());
        
        // Validate
        if (request.getPrice() == null) {
            throw new com.springboot.jenka_coffee.exception.BusinessRuleException("Giá sản phẩm không được để trống");
        }
        if (request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new com.springboot.jenka_coffee.exception.BusinessRuleException("Giá sản phẩm không thể âm");
        }
        
        // Get category
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + categoryId));
        
        // Build product
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice().setScale(0, java.math.RoundingMode.HALF_UP));
        product.setAvailable(request.getAvailable() != null ? request.getAvailable() : true);
        product.setRequireContact(request.getRequireContact() != null ? request.getRequireContact() : false);
        product.setCategory(category);
        product.setId(null); // Force INSERT
        
        // Create product (auto-generate slug)
        Product saved = create(product);
        
        // Upload image if provided
        if (imageFile != null && !imageFile.isEmpty()) {
            saved = saveProduct(saved, imageFile);
        }
        
        return saved;
    }
    
    /**
     * Update product from request parameters with validation
     * FIX BUG: price luôn null - giờ validate và xử lý đúng
     */
    @Override
    @Transactional
    public Product updateProductFromRequest(Integer id, String name, String description, BigDecimal price,
                                           String categoryId, Boolean available, MultipartFile imageFile) {
        log.info("Updating product ID: {}", id);
        
        // Validate price - FIX BUG Ở ĐÂY!
        if (price == null) {
            throw new com.springboot.jenka_coffee.exception.BusinessRuleException("Giá sản phẩm không được để trống");
        }
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new com.springboot.jenka_coffee.exception.BusinessRuleException("Giá sản phẩm không thể âm");
        }
        
        // Get existing product
        Product existing = findById(id);
        
        // Get category
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + categoryId));
        
        // Update fields
        existing.setName(name);
        existing.setDescription(description);
        existing.setPrice(price.setScale(0, java.math.RoundingMode.HALF_UP)); // Không còn null!
        existing.setAvailable(available);
        existing.setCategory(category);
        
        // Save with image
        return saveProduct(existing, imageFile);
    }
    
    /**
     * Delete product with business rule validation
     */
    @Override
    @Transactional
    public void deleteProductWithValidation(Integer id) {
        log.info("Deleting product with validation: {}", id);
        
        // Check if product exists
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + id);
        }
        
        // Business rule: Check if product is used in orders
        long orderCount = productRepository.countOrdersByProductId(id);
        if (orderCount > 0) {
            throw new com.springboot.jenka_coffee.exception.BusinessRuleException(
                "Không thể xóa sản phẩm này vì đã có " + orderCount + 
                " đơn hàng sử dụng. Bạn có thể ẩn sản phẩm thay vì xóa."
            );
        }
        
        delete(id);
    }
    
    /**
     * Toggle product featured status
     */
    @Override
    @Transactional
    public Product toggleFeatured(Integer id) {
        log.info("Toggling featured status for product ID: {}", id);
        
        Product product = findById(id);
        product.setFeatured(!Boolean.TRUE.equals(product.getFeatured()));
        
        return update(product);
    }
}
