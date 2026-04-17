package com.springboot.jenka_coffee.api.admin;

// ============================================================================
// PHASE 1 SCOPE REDUCTION: Booking feature temporarily disabled
// ============================================================================
// Customer decided to focus on core e-commerce flow first.
// Booking functionality will be re-enabled in Phase 2.
// ============================================================================

/*
import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.entity.ServiceBooking;
import com.springboot.jenka_coffee.service.BookingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/booking")
public class ApiAdminBookingController {

    private final BookingService bookingService;

    private enum BookingStatus { PENDING, CONFIRMED, COMPLETED, CANCELLED }

    public ApiAdminBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        size = Math.min(Math.max(size, 1), 100);
        page = Math.max(page, 0);

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<ServiceBooking> bookingPage = bookingService.findAll(pageable);

        Map<String, Object> data = new HashMap<>();
        data.put("items", bookingPage.getContent());
        data.put("currentPage", bookingPage.getNumber());
        data.put("totalPages", bookingPage.getTotalPages());
        data.put("totalItems", bookingPage.getTotalElements());

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đặt lịch thành công", data));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<String>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        try {
            BookingStatus.valueOf(status.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Trạng thái không hợp lệ. Chỉ chấp nhận: PENDING, CONFIRMED, COMPLETED, CANCELLED"));
        }

        bookingService.updateStatus(id, status.toUpperCase().trim());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công!", "OK"));
    }
}
*/
