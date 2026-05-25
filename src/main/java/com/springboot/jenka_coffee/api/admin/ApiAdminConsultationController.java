package com.springboot.jenka_coffee.api.admin;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.dto.request.ConsultationStatusUpdateRequest;
import com.springboot.jenka_coffee.dto.response.ConsultationResponse;
import com.springboot.jenka_coffee.entity.ConsultationRequest;
import com.springboot.jenka_coffee.entity.ConsultationStatus;
import com.springboot.jenka_coffee.service.ConsultationRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/consultations")
@RequiredArgsConstructor
public class ApiAdminConsultationController {

    private final ConsultationRequestService consultationRequestService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {

        if (page < 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Số trang không được âm"));
        }
        if (size <= 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Kích thước trang phải lớn hơn 0"));
        }
        if (size > 100) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Kích thước trang không được vượt quá 100"));
        }

        ConsultationStatus filterStatus = parseStatus(status);
        Page<ConsultationRequest> consultationPage = consultationRequestService.findAll(filterStatus, PageRequest.of(page, size));

        Map<String, Object> data = new HashMap<>();
        data.put("items", consultationPage.getContent().stream().map(this::toResponse).toList());
        data.put("currentPage", consultationPage.getNumber());
        data.put("totalPages", consultationPage.getTotalPages());
        data.put("totalItems", consultationPage.getTotalElements());

        return ResponseEntity.ok(ApiResponse.success("OK", data));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ConsultationResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ConsultationStatusUpdateRequest request) {
        ConsultationRequest consultation = consultationRequestService.updateStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái tư vấn thành công", toResponse(consultation)));
    }

    private ConsultationStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ConsultationStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Bộ lọc trạng thái không hợp lệ");
        }
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
