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
        // VULN-019 FIX: quantity = 0 → không giới hạn; quantity > 0 → còn lượt; quantity < 0 → hết
        // Logic cũ: (quantity > 0 && quantity <= 0) → NEVER TRUE → bug!
        if (v.getQuantity() != null && v.getQuantity() != 0 && v.getQuantity() <= 0) {
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
        // VULN-019 FIX: Pessimistic lock TRƯỚC khi check quantity — đóng race condition window
        Voucher v = entityManager.find(Voucher.class, voucherCode.toUpperCase().trim(),
                LockModeType.PESSIMISTIC_WRITE);
        if (v == null) return;

        // Re-validate sau khi lock (fresh data, không stale)
        if (v.getExpirationDate().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Mã giảm giá đã hết hạn!");
        }
        if (!Boolean.TRUE.equals(v.getActive())) {
            throw new BusinessRuleException("Mã giảm giá không còn hiệu lực!");
        }

        // quantity = 0 → không giới hạn, không trừ
        if (v.getQuantity() != null && v.getQuantity() != 0) {
            if (v.getQuantity() <= 0) {
                throw new BusinessRuleException("Mã giảm giá đã hết lượt sử dụng!");
            }
            v.setQuantity(v.getQuantity() - 1);
            if (v.getQuantity() == 0) v.setActive(false); // auto-deactivate
            entityManager.merge(v);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Voucher checkVoucher(String code, BigDecimal subtotal) {
        return validateForCheckout(code, subtotal);
    }
}
