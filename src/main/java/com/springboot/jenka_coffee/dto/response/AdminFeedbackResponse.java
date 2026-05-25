package com.springboot.jenka_coffee.dto.response;

import com.springboot.jenka_coffee.entity.FeedbackStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminFeedbackResponse {
    private Long id;
    private String fullname;
    private String phone;
    private String branch;
    private Integer rating;
    private String content;
    private FeedbackStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
}
