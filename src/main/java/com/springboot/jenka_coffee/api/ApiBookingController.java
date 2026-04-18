package com.springboot.jenka_coffee.api;

// ============================================================================
// PHASE 1 SCOPE REDUCTION: Booking feature temporarily disabled
// ============================================================================
// Customer decided to focus on core e-commerce flow first.
// Booking functionality will be re-enabled in Phase 2.
// ============================================================================

/*
import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.dto.request.BookingRequest;
import com.springboot.jenka_coffee.entity.ServiceBooking;
import com.springboot.jenka_coffee.service.BookingService;
import com.springboot.jenka_coffee.service.EmailService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/booking")
public class ApiBookingController {

    private final BookingService bookingService;
    private final EmailService emailService;

    public ApiBookingController(BookingService bookingService, EmailService emailService) {
        this.bookingService = bookingService;
        this.emailService = emailService;
    }

    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<Void>> submitBooking(@Valid @RequestBody BookingRequest request) {
        try {
            if (request.getBookingDate() != null &&
                    request.getBookingDate().isBefore(java.time.LocalDate.now())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Không thể đặt lịch trong quá khứ!"));
            }
            
            if (request.getBookingDate() != null &&
                    request.getBookingDate().isAfter(java.time.LocalDate.now().plusMonths(3))) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Không thể đặt lịch quá 3 tháng trong tương lai!"));
            }

            ServiceBooking booking = new ServiceBooking();
            booking.setCustomerName(sanitize(request.getCustomerName()));
            booking.setPhone(request.getPhone());
            booking.setDescription(sanitize(request.getDescription()));
            booking.setBookingDate(request.getBookingDate() != null
                    ? request.getBookingDate().atStartOfDay()
                    : LocalDateTime.now().plusDays(1));
            booking.setStatus("PENDING");

            bookingService.save(booking);

            try {
                emailService.sendBookingConfirmation(
                        booking.getCustomerName(),
                        booking.getPhone(),
                        booking.getBookingDate().toString(),
                        booking.getDescription()
                );
            } catch (Exception e) {
                log.warn("Failed to send booking email for phone={}", booking.getPhone());
            }

            return ResponseEntity.ok(
                    ApiResponse.success("Đặt lịch thành công, chúng tôi sẽ sớm liên hệ xác nhận!", null));
        } catch (Exception e) {
            log.error("Booking submit error", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Có lỗi xảy ra khi đặt lịch. Vui lòng thử lại!"));
        }
    }

    private static final org.owasp.html.PolicyFactory SANITIZE_POLICY =
            org.owasp.html.Sanitizers.FORMATTING.and(org.owasp.html.Sanitizers.LINKS);

    private String sanitize(String input) {
        if (input == null) return null;
        return SANITIZE_POLICY.sanitize(input).trim();
    }
}
*/
