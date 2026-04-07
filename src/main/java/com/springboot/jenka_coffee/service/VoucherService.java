package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.entity.Voucher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

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
     * Trừ 1 lượt dùng (quantity > 0). Dùng pessimistic lock để tránh race condition.
     */
    void consumeVoucher(String voucherCode);

    /** Public API: kiểm tra mã giảm giá từ phía khách hàng */
    Voucher checkVoucher(String code, BigDecimal subtotal);
}
