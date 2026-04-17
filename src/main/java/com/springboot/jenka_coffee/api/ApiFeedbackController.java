package com.springboot.jenka_coffee.api;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.dto.request.StoreFeedbackRequest;
import com.springboot.jenka_coffee.entity.StoreFeedback;
import com.springboot.jenka_coffee.service.StoreFeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/feedbacks")
@RequiredArgsConstructor
public class ApiFeedbackController {

    private final StoreFeedbackService feedbackService;

    @PostMapping
    public ResponseEntity<ApiResponse<StoreFeedback>> submitFeedback(
            @Valid @RequestBody StoreFeedbackRequest request) {
        try {
            StoreFeedback feedback = feedbackService.create(request);
            log.info("New feedback submitted: branch={}, rating={}/{}", 
                    feedback.getBranch(), feedback.getStoreRating(), feedback.getStaffRating());
            
            return ResponseEntity.ok(
                    ApiResponse.success("Cảm ơn bạn đã đánh giá! Chúng tôi sẽ cải thiện dịch vụ tốt hơn.", feedback));
        } catch (Exception e) {
            log.error("Failed to submit feedback", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Có lỗi xảy ra. Vui lòng thử lại sau!"));
        }
    }
}
