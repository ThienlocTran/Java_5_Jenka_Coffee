package com.springboot.jenka_coffee.api;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.entity.ServiceBooking;
import com.springboot.jenka_coffee.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/booking")
public class ApiBookingController {

    private final BookingService bookingService;

    public ApiBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<ServiceBooking>> submitBooking(@RequestBody ServiceBooking booking) {
        try {
            booking.setStatus("PENDING"); // Mặc định là Pending (Chờ xác nhận)
            ServiceBooking savedBooking = bookingService.save(booking);
            return ResponseEntity
                    .ok(ApiResponse.success("Đặt lịch thành công, chúng tôi sẽ sớm liên hệ xác nhận!", savedBooking));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Có lỗi xảy ra khi đặt lịch: " + e.getMessage()));
        }
    }
}
