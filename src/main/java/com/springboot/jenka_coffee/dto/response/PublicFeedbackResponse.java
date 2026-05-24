package com.springboot.jenka_coffee.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PublicFeedbackResponse {
    private Long id;
    private String fullname;
    private String branch;
    private Integer rating;
    private String content;
    private LocalDateTime createdAt;
}
