package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.dto.request.ProductRequest;
import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.entity.ProductImage;
import com.springboot.jenka_coffee.exception.BusinessRuleException;
import com.springboot.jenka_coffee.exception.ResourceNotFoundException;
import com.springboot.jenka_coffee.repository.CategoryRepository;
import com.springboot.jenka_coffee.repository.ProductRepository;
import com.springboot.jenka_coffee.repository.ProductImageRepository;
import com.springboot.jenka_coffee.service.ProductService;
import com.springboot.jenka_coffee.service.UploadService;
import com.springboot.jenka_coffee.service.VercelWebhookService;
import com.springboot.jenka_coffee.util.SlugUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

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

    // VULN #9 FIX: Connection Pool Exhaustion Prevention
    // PROBLEM: @Transactional holds DB connection during Cloudinary upload (1-3s network I/O)
    // - Multiple concurrent uploads can exhaust HikariCP connection pool (~10 connections)
    // - Other requests fail with ConnectionTimeoutException → application-wide DoS
    // 
    // SOLUTION: Upload image OUTSIDE transaction, then save to DB in separate transaction
    // - Step 1: Upload to Cloudinary (no DB connection held)
    // - Step 2: Save product to DB with image URL (@Transactional)
    // - Tradeoff: If DB save fails after upload, orphaned image in Cloudinary (logged for cleanup)
    @Override
    @CacheEvict(value = "categoryCounts", allEntries = true)
    public Product saveProduct(Product product, MultipartFile file) {
        log.info("Saving product: {} with image: {}", product.getName(),
                file != null ? file.getOriginalFilename() : "no image");

        // STEP 1: Upload image OUTSIDE transaction (no DB connection held during network I/O)
        String newImageUrl = null;
        String oldImageUrl = null;
        
        if (product.getId() != null) {
            // Get old image URL for cleanup (quick query, no transaction needed)
            Product existingProduct = productRepository.findById(product.getId()).orElse(null);
            if (existingProduct != null) {
                oldImageUrl = existingProduct.getImage();
            }
        }
        
        if (file != null && !file.isEmpty()) {
            try {
                // Network I/O happens HERE - no DB connection held
                newImageUrl = uploadService.saveProductImage(file);
                if (newImageUrl != null) {
                    product.setImage(newImageUrl);
                    log.info("Successfully uploaded product image: {}", newImageUrl);
                } else {
                    log.warn("Failed to upload product image for: {}", product.getName());
                }
            } catch (Exception e) {
                log.error("Error uploading product image: {}", e.getMessage(), e);
                // Continue with save even if upload fails
            }
        }

        // STEP 2: Save to database in transaction (fast, no network I/O)
        Product savedProduct = saveProductToDatabase(product);
        
        // STEP 3: Cleanup old image after successful save
        if (newImageUrl != null && oldImageUrl != null && !oldImageUrl.isEmpty() && !oldImageUrl.equals(newImageUrl)) {
            try {
                uploadService.deleteImage(oldImageUrl);
                log.info("Deleted old product image: {}", oldImageUrl);
            } catch (Exception e) {
                log.warn("Failed to delete old product image: {}", e.getMessage());
            }
        }
        
        // STEP 4: Trigger webhook (outside transaction, non-blocking)
        try {
            vercelWebhookService.triggerRebuild();
        } catch (Exception e) {
            log.warn("Failed to trigger Vercel rebuild: {}", e.getMessage());
        }
        
        return productRepository.findByIdWithCategory(savedProduct.getId())
                .orElse(savedProduct);
    }
    
    /**
     * VULN #9 FIX: Separate transactional method for DB operations only
     * This method ONLY does database operations - no network I/O
     * Transaction is short-lived, connection released quickly
     */
    @Transactional
    protected Product saveProductToDatabase(Product product) {
        Product savedProduct = productRepository.save(product);
        log.info("Successfully saved product to database with ID: {}", savedProduct.getId());
        return savedProduct;
    }

    // VULN #21 FIX: Connection Pool Exhaustion - Multi-Image Upload (REGRESSION)
    // PROBLEM: @Transactional holds DB connection during multiple Cloudinary uploads
    // - 5 images × 2 seconds each = 10 seconds with DB connection held
    // - 2-3 admins uploading simultaneously = connection pool exhausted
    // SOLUTION: Upload ALL images outside transaction, then save to DB in one transaction
    // TC-PRD-CTRL-043B FIX: Propagate exceptions instead of swallowing them
    public void saveProductImages(Integer productId, List<MultipartFile> imageFiles) {
        // STEP 1: Upload all images OUTSIDE transaction (no DB connection held)
        List<String> uploadedUrls = new ArrayList<>();
        List<String> failedFiles = new ArrayList<>();
        
        for (MultipartFile file : imageFiles) {
            if (file != null && !file.isEmpty()) {
                try {
                    String imageUrl = uploadService.saveProductImage(file);
                    if (imageUrl != null) {
                        uploadedUrls.add(imageUrl);
                        log.info("Successfully uploaded product image: {}", imageUrl);
                    } else {
                        failedFiles.add(file.getOriginalFilename());
                        log.error("Upload service returned null for file: {}", file.getOriginalFilename());
                    }
                } catch (Exception e) {
                    failedFiles.add(file.getOriginalFilename());
                    log.error("Error uploading product image '{}': {}", file.getOriginalFilename(), e.getMessage(), e);
                    // TC-PRD-CTRL-043B FIX: Propagate exception instead of silent fail
                    throw new RuntimeException("Storage service unavailable: " + e.getMessage(), e);
                }
            }
        }
        
        // Check if any uploads failed
        if (!failedFiles.isEmpty()) {
            throw new RuntimeException("Failed to upload images: " + String.join(", ", failedFiles));
        }
        
        // STEP 2: Save all image URLs to database in single transaction (fast)
        if (!uploadedUrls.isEmpty()) {
            saveProductImagesToDB(productId, uploadedUrls);
        }
    }
    
    /**
     * VULN #21 FIX: Separate transactional method for DB operations only
     * Saves multiple image URLs to database in a single transaction
     */
    @Transactional
    protected void saveProductImagesToDB(Integer productId, List<String> imageUrls) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        int order = 0;
        for (String imageUrl : imageUrls) {
            ProductImage productImage = new ProductImage();
            productImage.setProduct(product);
            productImage.setImageUrl(imageUrl);
            productImage.setDisplayOrder(order++);
            productImage.setIsPrimary(order == 1); // First image is primary
            productImageRepository.save(productImage);
        }
        
        log.info("Saved {} product images to database for product ID: {}", imageUrls.size(), productId);
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
            } catch (DataIntegrityViolationException e) {
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
                    // TC-PRD-CTRL-039 FIX: Throw BusinessRuleException instead of RuntimeException
                    // Controller catches BusinessRuleException → 400 Bad Request
                    // RuntimeException falls through to catch(Exception) → 500 Internal Server Error
                    log.error("Failed to create product after {} attempts due to slug collision", maxAttempts);
                    throw new BusinessRuleException("Sản phẩm với tên này đã tồn tại. Vui lòng chọn tên khác.");
                }
            }
        }
        
        // TC-PRD-CTRL-039 FIX: This should never be reached, but if it does, throw BusinessRuleException
        throw new BusinessRuleException("Không thể tạo sản phẩm sau nhiều lần thử. Vui lòng thử lại.");
    }

    @Override
    @CacheEvict(value = "categoryCounts", allEntries = true)
    public Product update(Product product) {
         //log.info("Updating product with ID: {}", product.getId());

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
      //  log.info("Successfully updated product with ID: {}", updatedProduct.getId());
        
        // VULN-M04 FIX: Trigger Vercel rebuild outside transaction
        try {
            vercelWebhookService.triggerRebuild();
        } catch (Exception e) {
            log.warn("Failed to trigger Vercel rebuild: {}", e.getMessage());
        }
        
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
        
        // VULN-M04 FIX: Trigger Vercel rebuild outside transaction
        try {
            vercelWebhookService.triggerRebuild();
        } catch (Exception e) {
            log.warn("Failed to trigger Vercel rebuild: {}", e.getMessage());
        }
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
        if (isDefaultHomepageQuery(categoryId, minPrice, maxPrice, keyword) && isDefaultProductSort(pageable)) {
            return findHomepagePinnedProducts(pageable);
        }
        return productRepository.findByAllCriteria(categoryId, minPrice, maxPrice, keyword, pageable);
    }

    private boolean isDefaultHomepageQuery(String categoryId, BigDecimal minPrice, BigDecimal maxPrice, String keyword) {
        return (categoryId == null || categoryId.isBlank())
                && minPrice == null
                && maxPrice == null
                && (keyword == null || keyword.isBlank());
    }

    private boolean isDefaultProductSort(Pageable pageable) {
        if (pageable == null || pageable.getSort().isUnsorted()) {
            return true;
        }
        return pageable.getSort().stream()
                .anyMatch(order -> "id".equals(order.getProperty()) && order.isDescending());
    }

    private Page<Product> findHomepagePinnedProducts(Pageable pageable) {
        List<Product> allProducts = productRepository.findAvailableForHomepageOrdering();
        int total = allProducts.size();
        if (total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<Product> arranged = new ArrayList<>(Collections.nCopies(total, null));
        List<Product> featuredProducts = allProducts.stream()
                .filter(p -> Boolean.TRUE.equals(p.getFeatured()) && p.getFeaturedPosition() != null)
                .sorted((a, b) -> a.getFeaturedPosition().compareTo(b.getFeaturedPosition()))
                .toList();
        java.util.Set<Integer> pinnedIds = featuredProducts.stream()
                .map(Product::getId)
                .collect(java.util.stream.Collectors.toSet());

        for (Product product : featuredProducts) {
            int index = Math.min(product.getFeaturedPosition(), total) - 1;
            while (index < total && arranged.get(index) != null) {
                index++;
            }
            if (index < total) {
                arranged.set(index, product);
            }
        }

        var regularIterator = allProducts.stream()
                .filter(p -> !pinnedIds.contains(p.getId()))
                .iterator();

        for (int i = 0; i < arranged.size(); i++) {
            if (arranged.get(i) == null && regularIterator.hasNext()) {
                arranged.set(i, regularIterator.next());
            }
        }

        List<Product> ordered = arranged.stream()
                .filter(Objects::nonNull)
                .toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), ordered.size());
        List<Product> pageContent = start >= ordered.size() ? List.of() : ordered.subList(start, end);

        return new PageImpl<>(pageContent, pageable, ordered.size());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> searchProductsPaginated(String keyword, Pageable pageable) {
        String keywordPattern = null;
        if (keyword != null && !keyword.trim().isEmpty()) {
            keywordPattern = "%" + keyword.trim().toLowerCase() + "%";
        }
        return productRepository.searchProductsPaginated(keywordPattern, pageable);
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
     * PROBLEM: synchronized keyword only works within single JVM instance
     * In load-balanced/multi-server deployments, two servers can generate same slug simultaneously
     * causing DataIntegrityViolationException when both try to insert
     * SOLUTION: Use optimistic retry approach - catch duplicate key exception and retry with new slug
     * This works across distributed systems without requiring Redis/distributed locks
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
     * SELF-INVOCATION FIX: Don't call saveProduct() from here (both have @Transactional)
     */
    @Override
    @Transactional
    public Product createProductFromRequest(ProductRequest request,
                                            String categoryId, MultipartFile imageFile) {
        log.info("Creating product from request: {}", request.getName());
        
        // Validate name
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BusinessRuleException("Tên sản phẩm không được để trống");
        }
        
        // Validate price
        if (request.getPrice() == null) {
            throw new BusinessRuleException("Giá sản phẩm không được để trống");
        }
        if (request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Giá sản phẩm không thể âm");
        }
        
        // Get category
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + categoryId));

        // Build product entity
        Product product = buildProductFromRequest(request, category);

        // Create product (auto-generate slug)
        Product saved = create(product);
        
        // Upload image if provided (inline to avoid self-invocation)
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String imageUrl = uploadService.saveProductImage(imageFile);
                if (imageUrl != null) {
                    saved.setImage(imageUrl);
                    saved = productRepository.save(saved);
                    log.info("Successfully uploaded product image: {}", imageUrl);
                }
            } catch (Exception e) {
                // TC-DATA-001 FIX: Re-throw exception to trigger @Transactional rollback.
                // Previously: exception was caught and swallowed → product saved without image → 200 returned.
                // Now: RuntimeException propagates → @Transactional rolls back product INSERT → 500 returned.
                log.error("Image upload failed for product '{}': {}", request.getName(), e.getMessage(), e);
                throw new RuntimeException("Không thể tạo sản phẩm: lỗi upload ảnh - " + e.getMessage(), e);
            }
        }
        
        return saved;
    }

    /**
     * Build Product entity from request DTO
     */
    private static Product buildProductFromRequest(ProductRequest request, Category category) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice().setScale(0, RoundingMode.HALF_UP));
        product.setAvailable(request.getAvailable() != null ? request.getAvailable() : true);
        product.setRequireContact(request.getRequireContact() != null ? request.getRequireContact() : false);
        product.setCategory(category);
        product.setId(null); // Force INSERT
        return product;
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
            throw new BusinessRuleException("Giá sản phẩm không được để trống");
        }
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Giá sản phẩm không thể âm");
        }
        
        // Get existing product
        Product existing = findById(id);
        
        // Get category
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + categoryId));
        
        // Update fields
        existing.setName(name);
        existing.setDescription(description);
        existing.setPrice(price.setScale(0, RoundingMode.HALF_UP)); // Không còn null!
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
            throw new BusinessRuleException(
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
        boolean nextFeatured = !Boolean.TRUE.equals(product.getFeatured());
        product.setFeatured(nextFeatured);
        product.setFeaturedPosition(nextFeatured ? nextFeaturedPosition() : null);
        return update(product);
    }

    @Override
    @Transactional
    public Product updateFeaturedPosition(Integer id, Integer position) {
        Product product = findById(id);

        if (position == null || position < 1) {
            product.setFeatured(false);
            product.setFeaturedPosition(null);
            return update(product);
        }

        List<Product> featuredProducts = productRepository.findFeaturedProductsForOrdering().stream()
                .filter(p -> !p.getId().equals(id))
                .toList();

        for (Product featuredProduct : featuredProducts) {
            Integer currentPosition = featuredProduct.getFeaturedPosition();
            if (currentPosition != null && currentPosition >= position) {
                featuredProduct.setFeaturedPosition(currentPosition + 1);
            }
        }
        productRepository.saveAll(featuredProducts);

        product.setFeatured(true);
        product.setFeaturedPosition(position);
        return update(product);
    }

    private Integer nextFeaturedPosition() {
        return productRepository.findFeaturedProductsForOrdering().stream()
                .map(Product::getFeaturedPosition)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .map(position -> position + 1)
                .orElse(1);
    }
}
