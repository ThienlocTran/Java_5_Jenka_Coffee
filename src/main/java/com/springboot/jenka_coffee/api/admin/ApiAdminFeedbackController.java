package com.springboot.jenka_coffee.api.admin;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.entity.StoreFeedback;
import com.springboot.jenka_coffee.service.StoreFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam(required = false) String branch) {
        
        size = Math.min(Math.max(size, 1), 100);
        page = Math.max(page, 0);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<StoreFeedback> feedbackPage;
        
        if (branch != null && !branch.isBlank()) {
            feedbackPage = feedbackService.findByBranch(branch, pageable);
        } else {
            feedbackPage = feedbackService.findAll(pageable);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("items", feedbackPage.getContent());
        data.put("currentPage", feedbackPage.getNumber());
        data.put("totalPages", feedbackPage.getTotalPages());
        data.put("totalItems", feedbackPage.getTotalElements());

        return ResponseEntity.ok(ApiResponse.success("OK", data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        feedbackService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa đánh giá thành công", null));
    }
}
