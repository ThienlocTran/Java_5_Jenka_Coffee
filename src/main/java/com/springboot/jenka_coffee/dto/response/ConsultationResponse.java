package com.springboot.jenka_coffee.dto.response;

import com.springboot.jenka_coffee.entity.ConsultationBudget;
import com.springboot.jenka_coffee.entity.ConsultationInterest;
import com.springboot.jenka_coffee.entity.ConsultationNeedType;
import com.springboot.jenka_coffee.entity.ConsultationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationResponse {
    private Long id;
    private String fullName;
    private String contactPhone;
    private ConsultationNeedType needType;
    private ConsultationInterest interest;
    private ConsultationBudget budget;
    private String note;
    private String source;
    private String productName;
    private String productUrl;
    private String pageTitle;
    private String pageUrl;
    private ConsultationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
