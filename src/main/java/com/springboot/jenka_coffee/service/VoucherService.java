package com.springboot.jenka_coffee.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface VoucherService {
    Page<Voucher> findAll(Pageable pageable);
    Voucher findByCode(String code);
    Voucher save(Voucher voucher);
    void delete(String code);
    Voucher toggle(String code);

    /**
     * Validate và trả về voucher nếu hợp lệ cho đơn hàng.
     * Ném exception nếu: không tồn tại, hết hạn, hết lượt, không đủ điều kiện đơn tối thiểu.
     */
    Voucher validateForCheckout(String voucherCode, BigDecimal orderSubtotal);

    /**
     * VULN-C02 FIX: Validate + acquire PESSIMISTIC_WRITE lock + consume trong 1 bước.
     * VULN-VOUCHER-SIPHON FIX: Check user đã dùng voucher này chưa.
     * Ngăn race condition giữa validate và consume.
     * 
     * @param voucherCode Mã voucher
     * @param orderSubtotal Tổng tiền đơn hàng
     * @param username Username của user (null nếu guest)
     * @return Voucher đã lock
     */
    Voucher validateAndLockVoucher(String voucherCode, BigDecimal orderSubtotal, String username);

    /**
     * Trừ 1 lượt dùng (quantity > 0). Dùng pessimistic lock để tránh race condition.
     */
    void consumeVoucher(String voucherCode);

    /** Public API: kiểm tra mã giảm giá từ phía khách hàng */
    Voucher checkVoucher(String code, BigDecimal subtotal);
}
