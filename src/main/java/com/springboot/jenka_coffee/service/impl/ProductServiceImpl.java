package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.repository.ProductDAO;
import com.springboot.jenka_coffee.service.ProductService;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {
    final
    ProductDAO dao;

    public ProductServiceImpl(ProductDAO dao) {
        this.dao = dao;
    }

    @Override
    public List<Product> findAll() {
        return dao.findAll();
    }

    @Override
    public Product findById(Integer id) {
        return dao.findById(id).orElse(null);
    }

    @Override
    public List<Product> findByCategoryId(String cid) {
        return dao.findByCategoryId(cid);
    }

    @Override
    public Product create(Product product) {
        return dao.save(product);
    }

    @Override
    public Product update(Product product) {
        return dao.save(product);
    }

    @Override
    public void delete(Integer id) {
        dao.deleteById(id);
    }
}