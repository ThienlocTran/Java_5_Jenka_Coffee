package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.dto.request.StoreFeedbackRequest;
import com.springboot.jenka_coffee.entity.FeedbackStatus;
import com.springboot.jenka_coffee.entity.StoreFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StoreFeedbackService {
    StoreFeedback create(StoreFeedbackRequest request);
    Page<StoreFeedback> findAll(String branch, FeedbackStatus status, Pageable pageable);
    List<StoreFeedback> findApproved(int limit);
    StoreFeedback updateStatus(Long id, FeedbackStatus status);
    void delete(Long id);
}
