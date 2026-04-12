package com.springboot.jenka_coffee.repository;

import com.springboot.jenka_coffee.entity.VoucherUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, Long> {
    
    /**
     * Check if user has already used this voucher
     */
    boolean existsByVoucherCodeAndUsername(String voucherCode, String username);
    
    /**
     * Find voucher usage by voucher code and username
     */
    Optional<VoucherUsage> findByVoucherCodeAndUsername(String voucherCode, String username);
    
    /**
     * Count total usage of a voucher
     */
    long countByVoucherCode(String voucherCode);
    
    /**
     * BUG-44 FIX: Count how many times a specific user has used a voucher
     * Used to enforce maxUsesPerUser limit
     */
    long countByVoucherCodeAndUsername(String voucherCode, String username);
}
