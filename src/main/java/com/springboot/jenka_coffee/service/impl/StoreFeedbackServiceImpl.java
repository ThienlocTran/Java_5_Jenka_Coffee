package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.dto.request.StoreFeedbackRequest;
import com.springboot.jenka_coffee.entity.StoreFeedback;
import com.springboot.jenka_coffee.exception.BusinessRuleException;
import com.springboot.jenka_coffee.repository.StoreFeedbackRepository;
import com.springboot.jenka_coffee.service.StoreFeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreFeedbackServiceImpl implements StoreFeedbackService {

    private final StoreFeedbackRepository feedbackRepository;
    
    // OWASP HTML Sanitizer to prevent XSS
    private static final org.owasp.html.PolicyFactory SANITIZE_POLICY =
            org.owasp.html.Sanitizers.FORMATTING.and(org.owasp.html.Sanitizers.LINKS);

    @Override
    @Transactional
    public StoreFeedback create(StoreFeedbackRequest request) {
        StoreFeedback feedback = new StoreFeedback();
        feedback.setBranch(request.getBranch().toUpperCase());
        feedback.setFullname(sanitize(request.getFullname()));
        feedback.setPhone(request.getPhone());
        feedback.setComment(sanitize(request.getComment()));
        feedback.setStoreRating(request.getStoreRating());
        feedback.setStaffRating(request.getStaffRating());
        
        return feedbackRepository.save(feedback);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StoreFeedback> findAll(Pageable pageable) {
        return feedbackRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StoreFeedback> findByBranch(String branch, Pageable pageable) {
        return feedbackRepository.findByBranchOrderByCreatedAtDesc(branch.toUpperCase(), pageable);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!feedbackRepository.existsById(id)) {
            throw new BusinessRuleException("Không tìm thấy đánh giá với ID: " + id);
        }
        feedbackRepository.deleteById(id);
    }

    private String sanitize(String input) {
        if (input == null) return null;
        return SANITIZE_POLICY.sanitize(input).trim();
    }
}
