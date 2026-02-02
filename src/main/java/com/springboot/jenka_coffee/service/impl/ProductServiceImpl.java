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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Override
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
    public Product findById(Integer id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
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
        log.info("Creating new product: {}", product.getName());
        Product savedProduct = productRepository.save(product);
        log.info("Successfully created product with ID: {}", savedProduct.getId());
        return savedProduct;
    }

    @Override
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
        log.info("Toggling availability for product ID: {}", id);
        
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        
        boolean newStatus = !product.getAvailable();
        product.setAvailable(newStatus);
        productRepository.save(product);
        
        log.info("Successfully toggled product ID {} availability to: {}", id, newStatus);
    }

    @Override
    public void deleteProduct(Integer id, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        log.info("Attempting to delete product with ID: {}", id);
        
        try {
            // Lấy thông tin sản phẩm trước khi xóa để hiển thị thông báo
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
            
            String productName = product.getName();
            
            // Thực hiện xóa
            productRepository.deleteById(id);
            
            // Thông báo thành công
            redirectAttributes.addFlashAttribute("successMessage", 
                "Đã xóa sản phẩm \"" + productName + "\" thành công!");
            
            log.info("Successfully deleted product: {} (ID: {})", productName, id);
            
        } catch (ResourceNotFoundException e) {
            log.error("Product not found for deletion: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Không tìm thấy sản phẩm cần xóa!");
                
        } catch (Exception e) {
            log.error("Error deleting product with ID {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Không thể xóa sản phẩm. Vui lòng thử lại!");
        }
    }
}