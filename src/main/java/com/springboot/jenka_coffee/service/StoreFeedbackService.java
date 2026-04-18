package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.dto.request.StoreFeedbackRequest;
import com.springboot.jenka_coffee.entity.StoreFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StoreFeedbackService {
    StoreFeedback create(StoreFeedbackRequest request);
    Page<StoreFeedback> findAll(Pageable pageable);
    Page<StoreFeedback> findByBranch(String branch, Pageable pageable);
    void delete(Long id);
}
