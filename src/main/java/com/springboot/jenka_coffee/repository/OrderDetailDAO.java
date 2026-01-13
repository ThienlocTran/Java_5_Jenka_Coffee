package com.springboot.jenka_coffee.repository;

import com.springboot.jenka_coffee.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderDetailDAO extends JpaRepository<OrderDetail, Long> {
}
