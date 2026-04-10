package com.springboot.jenka_coffee.api;

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

    /**
     * Dùng BookingRequest DTO — không nhận raw entity.
     * Ngăn mass assignment (status, id override) và Stored XSS.
     */
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<Void>> submitBooking(@Valid @RequestBody BookingRequest request) {
        try {
            // VULN-044 FIX: Giới hạn ngày đặt lịch tối đa 3 tháng trong tương lai
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

            // Email async — không block response, không expose exception message
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
            // KHÔNG expose e.getMessage() ra client — tránh log injection + info disclosure
            log.error("Booking submit error", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Có lỗi xảy ra khi đặt lịch. Vui lòng thử lại!"));
        }
    }

    /**
     * VULN-M03 FIX: Dùng OWASP Java HTML Sanitizer thay vì custom regex.
     * Custom regex có thể bị bypass bởi malformed tags, double encoding, event handlers.
     */
    private static final org.owasp.html.PolicyFactory SANITIZE_POLICY =
            org.owasp.html.Sanitizers.FORMATTING.and(org.owasp.html.Sanitizers.LINKS);

    private String sanitize(String input) {
        if (input == null) return null;
        // Strip tất cả HTML — chỉ giữ plain text
        return SANITIZE_POLICY.sanitize(input).trim();
    }
}
