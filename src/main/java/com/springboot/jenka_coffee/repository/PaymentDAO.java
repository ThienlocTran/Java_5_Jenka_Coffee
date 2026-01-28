package com.springboot.jenka_coffee.repository;

import com.springboot.jenka_coffee.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentDAO extends JpaRepository<Payment, Long> {

    // Tìm tất cả payment theo OrderId
    List<Payment> findByOrderId(Long orderId);

    // Tìm payment theo status
    List<Payment> findByStatus(String status);

    // Tìm payment theo phương thức thanh toán
    List<Payment> findByPaymentMethod(String paymentMethod);
}
