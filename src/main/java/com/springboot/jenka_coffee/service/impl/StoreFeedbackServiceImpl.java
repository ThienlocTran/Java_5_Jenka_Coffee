package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.dto.request.StoreFeedbackRequest;
import com.springboot.jenka_coffee.entity.FeedbackStatus;
import com.springboot.jenka_coffee.entity.StoreFeedback;
import com.springboot.jenka_coffee.exception.BusinessRuleException;
import com.springboot.jenka_coffee.repository.StoreFeedbackRepository;
import com.springboot.jenka_coffee.service.StoreFeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreFeedbackServiceImpl implements StoreFeedbackService {

    private static final String ANONYMOUS_NAME = "Khách hàng Jenka";

    private final StoreFeedbackRepository feedbackRepository;

    private static final org.owasp.html.PolicyFactory SANITIZE_POLICY =
            org.owasp.html.Sanitizers.FORMATTING.and(org.owasp.html.Sanitizers.LINKS);

    @Override
    @Transactional
    public StoreFeedback create(StoreFeedbackRequest request) {
        StoreFeedback feedback = new StoreFeedback();
        feedback.setBranch(normalizeBranch(request.getBranch()));
        feedback.setFullname(normalizeFullname(request.getFullname()));
        feedback.setPhone(normalizePhone(request.getPhone()));
        feedback.setComment(sanitize(request.getContent()));
        feedback.setRating(request.getRating());
        feedback.setStoreRating(request.getRating());
        feedback.setStaffRating(request.getRating());
        feedback.setStatus(FeedbackStatus.PENDING);
        feedback.setApprovedAt(null);
        return feedbackRepository.save(feedback);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StoreFeedback> findAll(String branch, FeedbackStatus status, Pageable pageable) {
        return feedbackRepository.search(normalizeBranch(branch), status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoreFeedback> findApproved(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 20);
        return feedbackRepository.findPublicByStatus(FeedbackStatus.APPROVED, PageRequest.of(0, safeLimit));
    }

    @Override
    @Transactional
    public StoreFeedback updateStatus(Long id, FeedbackStatus status) {
        StoreFeedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Không tìm thấy feedback với ID: " + id));
        feedback.setStatus(status);
        feedback.setApprovedAt(status == FeedbackStatus.APPROVED ? LocalDateTime.now() : null);
        return feedbackRepository.save(feedback);
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
        if (input == null) {
            return null;
        }
        String value = SANITIZE_POLICY.sanitize(input).trim();
        return value.isEmpty() ? null : value;
    }

    private String normalizeFullname(String fullname) {
        String value = sanitize(fullname);
        return value == null ? ANONYMOUS_NAME : value;
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String value = phone.replaceAll("[\\s.-]+", "").trim();
        return value.isEmpty() ? null : value;
    }

    private String normalizeBranch(String branch) {
        if (branch == null || branch.isBlank()) {
            return null;
        }
        return branch.trim().toUpperCase();
    }
}
