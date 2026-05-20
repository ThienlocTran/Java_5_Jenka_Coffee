package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.dto.request.ProductRequest;
import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.entity.ProductImage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ProductService {
    List<Product> findAll();

    Product findById(Integer id);
    
    Product findBySlug(String slug);

    Product create(Product product);

    Product update(Product product);

    void delete(Integer id);

    Product saveProduct(Product product, MultipartFile file);

    List<Product> getRelatedProducts(String categoryId, Integer productId);

    Map<String, Object> getProductDetail(Integer productId);

    Page<Product> findAllPaginated(Pageable pageable);

    Map<String, Long> getCategoryCounts();

    void toggleAvailable(Integer id);

    Page<Product> filterProductsWithAllCriteria(String categoryId, BigDecimal minPrice, BigDecimal maxPrice,
                                                String keyword, Pageable pageable);

    Page<Product> searchProductsPaginated(String keyword, Pageable pageable);
    
    // Admin operations with business logic
    Product createProductFromRequest(ProductRequest request, String categoryId, MultipartFile imageFile);
    
    Product updateProductFromRequest(Integer id, ProductRequest request, String categoryId, MultipartFile imageFile);
    
    void deleteProductWithValidation(Integer id);
    
    Product toggleFeatured(Integer id);

    Product updateFeaturedPosition(Integer id, Integer position);
    
    void saveProductImages(Integer productId, List<MultipartFile> images);
    
    List<ProductImage> getProductImages(Integer productId);
    
    void deleteProductImage(Integer imageId);
    
    Map<String, Object> generateSlugsForAllProducts();
}
