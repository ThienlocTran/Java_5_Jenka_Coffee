package com.springboot.jenka_coffee.repository;

import com.springboot.jenka_coffee.entity.StoreFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoreFeedbackRepository extends JpaRepository<StoreFeedback, Long> {
    Page<StoreFeedback> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<StoreFeedback> findByBranchOrderByCreatedAtDesc(String branch, Pageable pageable);
}
