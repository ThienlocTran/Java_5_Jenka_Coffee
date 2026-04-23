package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.dto.request.CheckoutRequest;
import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {

    Order create(Order order);
    Order findById(Long id);
    Order findByIdWithDetails(Long id);

    Page<Order> findByUsername(String username, Pageable pageable);
    Order checkout(CheckoutRequest request, Account account);
    void postCheckout(Order savedOrder, Account account);
    CheckoutRequest prepareCheckoutRequest(Account user);
    void updateStatus(Long orderId, int status);
    Page<Order> findAll(Pageable pageable);

    /**
     * Find orders with account eagerly loaded (avoids lazy proxy in serialization).
     * Used by admin controllers — keeps repositories out of controller layer.
     */
    List<Order> findAllWithAccountByIds(List<Long> ids);
}