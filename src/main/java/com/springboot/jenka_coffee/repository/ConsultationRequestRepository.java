package com.springboot.jenka_coffee.repository;

import com.springboot.jenka_coffee.entity.ConsultationRequest;
import com.springboot.jenka_coffee.entity.ConsultationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsultationRequestRepository extends JpaRepository<ConsultationRequest, Long> {
    Page<ConsultationRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<ConsultationRequest> findAllByStatusOrderByCreatedAtDesc(ConsultationStatus status, Pageable pageable);
}
