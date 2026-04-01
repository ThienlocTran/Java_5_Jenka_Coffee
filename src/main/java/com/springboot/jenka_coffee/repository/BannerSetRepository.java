package com.springboot.jenka_coffee.repository;

import com.springboot.jenka_coffee.entity.BannerSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface BannerSetRepository extends JpaRepository<BannerSet, Long> {
    Optional<BannerSet> findByActiveTrue();

    @Modifying
    @Transactional
    @Query("UPDATE BannerSet b SET b.active = false")
    void deactivateAll();
}
