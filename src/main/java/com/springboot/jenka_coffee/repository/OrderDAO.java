package com.springboot.jenka_coffee.repository;

import com.springboot.jenka_coffee.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderDAO extends JpaRepository<Order, Long> {
}
