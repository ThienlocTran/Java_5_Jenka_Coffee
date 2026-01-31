package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.dto.response.StockStatus;
import com.springboot.jenka_coffee.entity.Product;

import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

public interface ProductService {
    List<Product> findAll();

    Product findById(Integer id);

    List<Product> findByCategoryId(String cid); // Lọc mã theo loại

    Product create(Product product);

    Product update(Product product);

    void delete(Integer id);

    Product saveProduct(Product product, MultipartFile file);

    List<Product> getRelatedProducts(String categoryId, Integer productId);

    List<Product> filterProducts(String categoryId);

    Map<String, Object> getProductDetail(Integer productId);

    /**
     * Get stock status based on quantity
     * 
     * @param quantity Product quantity
     * @return StockStatus enum (IN_STOCK, LOW_STOCK, OUT_OF_STOCK)
     */
    StockStatus getStockStatus(Integer quantity);

    /**
     * Get human-readable stock message
     * 
     * @param quantity Product quantity
     * @return Display message for UI
     */
    String getStockMessage(Integer quantity);
}
