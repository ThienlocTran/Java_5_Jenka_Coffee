package com.springboot.jenka_coffee.api.admin;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.dto.request.FeedbackStatusUpdateRequest;
import com.springboot.jenka_coffee.dto.response.AdminFeedbackResponse;
import com.springboot.jenka_coffee.entity.FeedbackStatus;
import com.springboot.jenka_coffee.entity.StoreFeedback;
import com.springboot.jenka_coffee.service.StoreFeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/feedbacks")
@RequiredArgsConstructor
public class ApiAdminFeedbackController {

    private final StoreFeedbackService feedbackService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) FeedbackStatus status) {

        size = Math.min(Math.max(size, 1), 100);
        page = Math.max(page, 0);

        Pageable pageable = PageRequest.of(page, size);
        Page<StoreFeedback> feedbackPage = feedbackService.findAll(branch, status, pageable);

        Map<String, Object> data = new HashMap<>();
        data.put("items", feedbackPage.getContent().stream().map(this::toAdminResponse).toList());
        data.put("currentPage", feedbackPage.getNumber());
        data.put("totalPages", feedbackPage.getTotalPages());
        data.put("totalItems", feedbackPage.getTotalElements());

        return ResponseEntity.ok(ApiResponse.success("OK", data));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AdminFeedbackResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody FeedbackStatusUpdateRequest request) {
        StoreFeedback feedback = feedbackService.updateStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái feedback thành công", toAdminResponse(feedback)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        feedbackService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa đánh giá thành công", null));
    }

    private AdminFeedbackResponse toAdminResponse(StoreFeedback feedback) {
        return AdminFeedbackResponse.builder()
                .id(feedback.getId())
                .fullname(feedback.getFullname())
                .phone(feedback.getPhone())
                .branch(feedback.getBranch())
                .rating(resolveRating(feedback))
                .content(feedback.getComment())
                .status(feedback.getStatus())
                .createdAt(feedback.getCreatedAt())
                .approvedAt(feedback.getApprovedAt())
                .build();
    }

    private Integer resolveRating(StoreFeedback feedback) {
        if (feedback.getRating() != null) {
            return feedback.getRating();
        }
        if (feedback.getStoreRating() != null) {
            return feedback.getStoreRating();
        }
        return feedback.getStaffRating();
    }
}
