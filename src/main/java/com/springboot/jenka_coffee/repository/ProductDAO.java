package com.springboot.jenka_coffee.repository;

import com.springboot.jenka_coffee.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductDAO extends JpaRepository<Product, Integer> {
    List<Product> findByCategoryId(String cid);

    List<Product> findTop4ByCategoryIdAndIdNot(String categoryId, Integer id);
}