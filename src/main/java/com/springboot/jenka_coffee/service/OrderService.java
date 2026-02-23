package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.dto.request.CheckoutRequest;
import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {

    // Các hàm cơ bản
    Order create(Order order);
    Order findById(Long id);

    Page<Order> findByUsername(String username, Pageable pageable);
    // Hàm Checkout (Quan trọng: Phải có tham số Account)
    Order checkout(CheckoutRequest request, Account account);

    // Hàm hỗ trợ điền form
    CheckoutRequest prepareCheckoutRequest(Account user);

    // --- CÁC HÀM MỚI CHO ADMIN (Nguyên nhân gây lỗi nếu thiếu) ---

    // 1. Cập nhật trạng thái đơn hàng
    Order updateStatus(Long orderId, int status);

    // 2. Lấy danh sách phân trang (Cho Admin)
    Page<Order> findAll(Pageable pageable);
}