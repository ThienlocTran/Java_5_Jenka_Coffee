package com.springboot.jenka_coffee.repository;

import com.springboot.jenka_coffee.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryDAO extends JpaRepository<Category, String> {
}
