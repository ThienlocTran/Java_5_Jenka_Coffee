package com.springboot.jenka_coffee.api;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.entity.ServiceBooking;
import com.springboot.jenka_coffee.service.BookingService;
import com.springboot.jenka_coffee.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<ApiResponse<ServiceBooking>> submitBooking(@RequestBody ServiceBooking booking) {
        try {
            booking.setStatus("PENDING");
            ServiceBooking savedBooking = bookingService.save(booking);

            // Gửi email thông báo admin (async — không block response)
            try {
                String dateStr = savedBooking.getBookingDate() != null
                        ? savedBooking.getBookingDate().toString() : "Chưa xác định";
                emailService.sendBookingConfirmation(
                        savedBooking.getCustomerName(),
                        savedBooking.getPhone(),
                        dateStr,
                        savedBooking.getDescription()
                );
            } catch (Exception e) {
                log.warn("Failed to send booking email: {}", e.getMessage());
            }

            return ResponseEntity.ok(
                    ApiResponse.success("Đặt lịch thành công, chúng tôi sẽ sớm liên hệ xác nhận!", savedBooking));
        } catch (Exception e) {
            log.error("Booking error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Có lỗi xảy ra khi đặt lịch: " + e.getMessage()));
        }
    }
}
