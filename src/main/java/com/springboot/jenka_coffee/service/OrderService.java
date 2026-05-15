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

    /** Find order by its public code (e.g. "ORD-20260511-AB12CD"). Returns null if not found. */
    Order findByOrderCode(String orderCode);

    /** Find order by its public code with all details eagerly loaded. Throws ResourceNotFoundException if missing. */
    Order findByOrderCodeWithDetails(String orderCode);

    Page<Order> findByUsername(String username, Pageable pageable);
    Page<Order> findByUsernameWithDetails(String username, Pageable pageable);
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