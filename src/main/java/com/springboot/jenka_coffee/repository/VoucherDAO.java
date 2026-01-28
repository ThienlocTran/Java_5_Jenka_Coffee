package com.springboot.jenka_coffee.repository;

import com.springboot.jenka_coffee.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherDAO extends JpaRepository<Voucher, String> {

    // Tìm tất cả voucher đang active
    List<Voucher> findByActiveTrue();

    // Tìm voucher theo code và đang active
    Optional<Voucher> findByCodeAndActiveTrue(String code);
}
