package com.springboot.jenka_coffee.repository;

import com.springboot.jenka_coffee.entity.BannerSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * BannerSet Repository
 * Note: @Transactional should be at Service layer, not Repository
 */
public interface BannerSetRepository extends JpaRepository<BannerSet, Long> {
    Optional<BannerSet> findByActiveTrue();

    /**
     * Deactivate all active banners
     * Added WHERE clause for safety and future-proofing
     */
    @Modifying
    @Query("UPDATE BannerSet b SET b.active = false WHERE b.active = true")
    int deactivateAll();
}
