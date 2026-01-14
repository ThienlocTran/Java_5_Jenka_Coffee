package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.entity.Product;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public interface ProductService {
    List<Product> findAll();

    Product findById(Integer id);

    List<Product> findByCategoryId(String cid); // Lọc mã theo loại

    Product create(Product product);

    Product update(Product product);

    void delete(Integer id);

    Product saveProduct(Product product, MultipartFile file);
}
