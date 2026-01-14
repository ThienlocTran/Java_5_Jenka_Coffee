package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.repository.ProductDAO;
import com.springboot.jenka_coffee.service.ProductService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    final ProductDAO pdao;

    public ProductServiceImpl(ProductDAO pdao) {
        this.pdao = pdao;
    }

    @Override
    public List<Product> findAll() {
        return pdao.findAll();
    }

    @Override
    public Product findById(Integer id) {
        // .orElse(null) giúp tránh lỗi nếu ID không tồn tại
        return pdao.findById(id).orElse(null);
    }

    @Override
    public List<Product> findByCategoryId(String cid) {
        // Cách 1: Viết method trong DAO (Khuyên dùng)
        return pdao.findByCategoryId(cid);
    }

    @Override
    public Product create(Product product) {
        return pdao.save(product);
    }

    @Override
    public Product update(Product product) {
        return pdao.save(product);
    }

    @Override
    public void delete(Integer id) {
        pdao.deleteById(id);
    }
}