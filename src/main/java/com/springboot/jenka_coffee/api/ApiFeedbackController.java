package com.springboot.jenka_coffee.api;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.dto.request.StoreFeedbackRequest;
import com.springboot.jenka_coffee.dto.response.PublicFeedbackResponse;
import com.springboot.jenka_coffee.entity.StoreFeedback;
import com.springboot.jenka_coffee.service.StoreFeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/feedbacks")
@RequiredArgsConstructor
public class ApiFeedbackController {

    private final StoreFeedbackService feedbackService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> submitFeedback(
            @Valid @RequestBody StoreFeedbackRequest request) {
        StoreFeedback feedback = feedbackService.create(request);
        log.info("New feedback submitted: branch={}, rating={}", feedback.getBranch(), resolveRating(feedback));
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Cảm ơn bạn đã gửi góp ý. Jenka Coffee sẽ xem xét và phản hồi khi cần.",
                        null
                )
        );
    }

    @GetMapping("/approved")
    public ResponseEntity<ApiResponse<List<PublicFeedbackResponse>>> approved(
            @RequestParam(defaultValue = "6") int limit) {
        List<PublicFeedbackResponse> items = feedbackService.findApproved(limit)
                .stream()
                .map(this::toPublicResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("OK", items));
    }

    private PublicFeedbackResponse toPublicResponse(StoreFeedback feedback) {
        return PublicFeedbackResponse.builder()
                .id(feedback.getId())
                .fullname(feedback.getFullname())
                .branch(feedback.getBranch())
                .rating(resolveRating(feedback))
                .content(feedback.getComment())
                .createdAt(feedback.getCreatedAt())
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
