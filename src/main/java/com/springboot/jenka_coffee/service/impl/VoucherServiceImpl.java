package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.entity.Voucher;
import com.springboot.jenka_coffee.exception.BusinessRuleException;
import com.springboot.jenka_coffee.exception.ResourceNotFoundException;
import com.springboot.jenka_coffee.repository.VoucherRepository;
import com.springboot.jenka_coffee.service.VoucherService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;
    private final EntityManager entityManager;

    @Override
    public Page<Voucher> findAll(Pageable pageable) {
        return voucherRepository.findAll(pageable);
    }

    @Override
    public Voucher findByCode(String code) {
        return voucherRepository.findById(code)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher", "code", code));
    }

    @Override
    @Transactional
    public Voucher save(Voucher voucher) {
        // Normalize code: uppercase, no spaces
        voucher.setCode(voucher.getCode().toUpperCase().trim());
        if (voucher.getScope() == null) voucher.setScope("ALL");
        if ("ALL".equals(voucher.getScope())) voucher.setApplicableProductIds(null);
        return voucherRepository.save(voucher);
    }

    @Override
    @Transactional
    public void delete(String code) {
        Voucher v = findByCode(code);
        // Không xóa nếu đã có đơn hàng dùng voucher này
        if (v.getOrders() != null && !v.getOrders().isEmpty()) {
            throw new BusinessRuleException("Không thể xóa voucher đã được sử dụng trong đơn hàng!");
        }
        voucherRepository.deleteById(code);
    }

    @Override
    @Transactional
    public Voucher toggle(String code) {
        Voucher v = findByCode(code);
        v.setActive(!v.getActive());
        return voucherRepository.save(v);
    }

    @Override
    @Transactional(readOnly = true)
    public Voucher validateForCheckout(String voucherCode, BigDecimal orderSubtotal) {
        Voucher v = voucherRepository.findByCodeAndActiveTrue(voucherCode.toUpperCase().trim())
                .orElseThrow(() -> new BusinessRuleException("Mã giảm giá không hợp lệ hoặc đã hết hạn!"));

        if (v.getExpirationDate().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Mã giảm giá đã hết hạn!");
        }
        // quantity = 0 nghĩa là không giới hạn
        if (v.getQuantity() != null && v.getQuantity() > 0 && v.getQuantity() <= 0) {
            throw new BusinessRuleException("Mã giảm giá đã hết lượt sử dụng!");
        }
        if (v.getMinOrderAmount() != null && orderSubtotal.compareTo(v.getMinOrderAmount()) < 0) {
            throw new BusinessRuleException(
                    "Đơn hàng tối thiểu " + v.getMinOrderAmount().toPlainString() + "đ để dùng mã này!");
        }
        return v;
    }

    @Override
    @Transactional
    public void consumeVoucher(String voucherCode) {
        // Pessimistic lock — tránh race condition khi nhiều user dùng cùng lúc
        Voucher v = entityManager.find(Voucher.class, voucherCode.toUpperCase().trim(),
                LockModeType.PESSIMISTIC_WRITE);
        if (v == null) return;
        // Chỉ trừ nếu quantity > 0 (0 = không giới hạn)
        if (v.getQuantity() != null && v.getQuantity() > 0) {
            if (v.getQuantity() <= 0) {
                throw new BusinessRuleException("Mã giảm giá đã hết lượt sử dụng!");
            }
            v.setQuantity(v.getQuantity() - 1);
            // Tự động deactivate khi hết lượt
            if (v.getQuantity() == 0) v.setActive(false);
            entityManager.merge(v);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Voucher checkVoucher(String code, BigDecimal subtotal) {
        return validateForCheckout(code, subtotal);
    }
}
