package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.dto.request.CheckoutRequest;
import com.springboot.jenka_coffee.entity.Order;
import java.util.List;

public interface OrderService {
    Order create(Order order);

    Order findById(Long id);

    List<Order> findByUsername(String username); // Tìm đơn của 1 người

    /**
     * Complete checkout transaction: create order, save details, deduct inventory,
     * clear cart
     * 
     * @param request Checkout form data
     * @return Saved order
     * @throws com.springboot.jenka_coffee.exception.InsufficientStockException if
     *                                                                          stock
     *                                                                          unavailable
     */
    Order checkout(CheckoutRequest request);
}