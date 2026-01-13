package com.springboot.jenka_coffee.repository;

import com.springboot.jenka_coffee.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface ProductDAO extends JpaRepository<Product, Integer> {
    List<Product> findByCategoryId(String cid);
}