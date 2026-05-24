package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.dto.request.ConsultationCreateRequest;
import com.springboot.jenka_coffee.entity.ConsultationRequest;
import com.springboot.jenka_coffee.entity.ConsultationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ConsultationRequestService {
    ConsultationRequest create(ConsultationCreateRequest request);
    Page<ConsultationRequest> findAll(ConsultationStatus status, Pageable pageable);
    ConsultationRequest updateStatus(Long id, String status);
}
