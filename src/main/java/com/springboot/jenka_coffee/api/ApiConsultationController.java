package com.springboot.jenka_coffee.api;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.dto.request.ConsultationCreateRequest;
import com.springboot.jenka_coffee.dto.response.ConsultationResponse;
import com.springboot.jenka_coffee.entity.ConsultationRequest;
import com.springboot.jenka_coffee.service.ConsultationRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consultations")
@RequiredArgsConstructor
public class ApiConsultationController {

    private final ConsultationRequestService consultationRequestService;

    @PostMapping
    public ResponseEntity<ApiResponse<ConsultationResponse>> create(@Valid @RequestBody ConsultationCreateRequest request) {
        ConsultationRequest consultation = consultationRequestService.create(request);
        return ResponseEntity.ok(ApiResponse.success(
                "Cảm ơn bạn. Jenka Coffee sẽ gọi/Zalo lại để tư vấn trước khi xác nhận đơn.",
                toResponse(consultation)
        ));
    }

    private ConsultationResponse toResponse(ConsultationRequest consultation) {
        return ConsultationResponse.builder()
                .id(consultation.getId())
                .fullName(consultation.getFullName())
                .contactPhone(consultation.getContactPhone())
                .needType(consultation.getNeedType())
                .interest(consultation.getInterest())
                .budget(consultation.getBudget())
                .note(consultation.getNote())
                .source(consultation.getSource())
                .productName(consultation.getProductName())
                .productUrl(consultation.getProductUrl())
                .pageTitle(consultation.getPageTitle())
                .pageUrl(consultation.getPageUrl())
                .status(consultation.getStatus())
                .createdAt(consultation.getCreatedAt())
                .updatedAt(consultation.getUpdatedAt())
                .build();
    }
}
