package com.springboot.jenka_coffee.api.admin;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.repository.ContactRepository;
import com.springboot.jenka_coffee.repository.OrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/notifications")
public class ApiAdminNotificationController {

    private final OrderRepository orderRepository;

    private final ContactRepository contactRepository;

    public ApiAdminNotificationController(OrderRepository orderRepository,

                                           ContactRepository contactRepository) {
        this.orderRepository = orderRepository;

        this.contactRepository = contactRepository;
    }

    /**
     * GET /api/admin/notifications/counts
     * Trả về số lượng thông báo chưa xử lý:
     * - newOrders: đơn hàng status=0 (NEW)
     * - pendingBookings: lịch hẹn status=PENDING
     * - unreadContacts: liên hệ chưa đọc
     */
    @GetMapping("/counts")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getCounts() {
        long newOrders      = orderRepository.countByStatus(0);

        long unreadContacts = contactRepository.countByIsReadFalse();

        return ResponseEntity.ok(ApiResponse.success("OK", Map.of(
                "newOrders",       newOrders,

                "unreadContacts",  unreadContacts

        )));
    }
}
