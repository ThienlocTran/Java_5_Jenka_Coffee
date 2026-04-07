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
            // Map DTO → Entity (server-side set status, không tin client)
            ServiceBooking booking = new ServiceBooking();
            booking.setCustomerName(sanitize(request.getCustomerName()));
            booking.setPhone(request.getPhone());
            booking.setDescription(sanitize(request.getDescription()));
            booking.setBookingDate(request.getBookingDate() != null
                    ? request.getBookingDate().atStartOfDay()
                    : LocalDateTime.now().plusDays(1));
            booking.setStatus("PENDING"); // server-side only

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

    /** Strip HTML tags cơ bản — ngăn Stored XSS */
    private String sanitize(String input) {
        if (input == null) return null;
        return input.replaceAll("<[^>]*>", "").trim();
    }
}
