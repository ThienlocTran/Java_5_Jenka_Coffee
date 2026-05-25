package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.dto.request.ConsultationCreateRequest;
import com.springboot.jenka_coffee.entity.ConsultationBudget;
import com.springboot.jenka_coffee.entity.ConsultationInterest;
import com.springboot.jenka_coffee.entity.ConsultationNeedType;
import com.springboot.jenka_coffee.entity.ConsultationRequest;
import com.springboot.jenka_coffee.entity.ConsultationStatus;
import com.springboot.jenka_coffee.exception.ResourceNotFoundException;
import com.springboot.jenka_coffee.repository.ConsultationRequestRepository;
import com.springboot.jenka_coffee.service.ConsultationRequestService;
import lombok.RequiredArgsConstructor;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ConsultationRequestServiceImpl implements ConsultationRequestService {

    private static final PolicyFactory SANITIZE_POLICY = Sanitizers.FORMATTING.and(Sanitizers.LINKS);

    private final ConsultationRequestRepository consultationRequestRepository;

    @Override
    public ConsultationRequest create(ConsultationCreateRequest request) {
        String contactPhone = trimToNull(request.getContactPhone());
        if (!isValidVietnamPhoneOrZalo(contactPhone)) {
            throw new IllegalArgumentException("Vui lòng nhập số điện thoại Việt Nam hợp lệ hoặc Zalo có kèm số");
        }

        ConsultationRequest consultation = new ConsultationRequest();
        consultation.setFullName(trimToNull(request.getFullName()));
        consultation.setContactPhone(sanitizeText(contactPhone));
        consultation.setNeedType(parseNeedType(request.getNeedType()));
        consultation.setInterest(parseInterest(request.getInterest()));
        consultation.setBudget(parseBudget(request.getBudget()));
        consultation.setNote(trimToNull(request.getNote()));
        consultation.setSource(normalizeSource(request.getSource()));
        consultation.setProductName(trimToNull(request.getProductName()));
        consultation.setProductUrl(trimToNull(request.getProductUrl()));
        consultation.setPageTitle(trimToNull(request.getPageTitle()));
        consultation.setPageUrl(trimToNull(request.getPageUrl()));
        consultation.setStatus(ConsultationStatus.NEW);

        sanitizeEntity(consultation);
        return consultationRequestRepository.save(consultation);
    }

    @Override
    public Page<ConsultationRequest> findAll(ConsultationStatus status, Pageable pageable) {
        if (status == null) {
            return consultationRequestRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return consultationRequestRepository.findAllByStatusOrderByCreatedAtDesc(status, pageable);
    }

    @Override
    public ConsultationRequest updateStatus(Long id, String status) {
        ConsultationRequest consultation = consultationRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu tư vấn"));
        consultation.setStatus(parseStatus(status));
        return consultationRequestRepository.save(consultation);
    }

    private void sanitizeEntity(ConsultationRequest consultation) {
        consultation.setFullName(sanitizeNullable(consultation.getFullName()));
        consultation.setContactPhone(sanitizeNullable(consultation.getContactPhone()));
        consultation.setNote(sanitizeNullable(consultation.getNote()));
        consultation.setSource(sanitizeNullable(consultation.getSource()));
        consultation.setProductName(sanitizeNullable(consultation.getProductName()));
        consultation.setProductUrl(sanitizeNullable(consultation.getProductUrl()));
        consultation.setPageTitle(sanitizeNullable(consultation.getPageTitle()));
        consultation.setPageUrl(sanitizeNullable(consultation.getPageUrl()));
    }

    private String sanitizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String sanitized = SANITIZE_POLICY.sanitize(value).trim();
        return sanitized.isEmpty() ? null : sanitized;
    }

    private String sanitizeText(String value) {
        String sanitized = sanitizeNullable(value);
        if (sanitized == null) {
            throw new IllegalArgumentException("Thông tin liên hệ không hợp lệ");
        }
        return sanitized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeSource(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private boolean isValidVietnamPhoneOrZalo(String value) {
        if (value == null) {
            return false;
        }

        String normalizedDigits = value.replaceAll("[^\\d+]", "");
        if (normalizedDigits.matches("^(0\\d{9,10}|\\+84\\d{9,10}|84\\d{9,10})$")) {
            return true;
        }

        String normalizedText = value.toLowerCase(Locale.ROOT);
        return normalizedText.contains("zalo") && normalizedText.matches(".*(0\\d{9,10}|\\+84\\d{9,10}|84\\d{9,10}).*");
    }

    private ConsultationNeedType parseNeedType(String value) {
        try {
            return ConsultationNeedType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Nhu cầu sử dụng không hợp lệ");
        }
    }

    private ConsultationInterest parseInterest(String value) {
        try {
            return ConsultationInterest.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Hạng mục cần tư vấn không hợp lệ");
        }
    }

    private ConsultationBudget parseBudget(String value) {
        try {
            return ConsultationBudget.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Khoảng ngân sách không hợp lệ");
        }
    }

    private ConsultationStatus parseStatus(String value) {
        try {
            return ConsultationStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Trạng thái tư vấn không hợp lệ");
        }
    }
}
