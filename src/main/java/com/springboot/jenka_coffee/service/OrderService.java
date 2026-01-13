package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.entity.Order;
import java.util.List;

public interface OrderService {
    Order create(Order order);
    Order findById(Long id);
    List<Order> findByUsername(String username); // Tìm đơn của 1 người
}