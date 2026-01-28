package com.springboot.jenka_coffee.repository;

import com.springboot.jenka_coffee.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
}
