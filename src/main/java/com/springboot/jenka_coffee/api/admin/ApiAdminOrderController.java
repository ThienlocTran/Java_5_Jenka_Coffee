package com.springboot.jenka_coffee.api.admin;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.entity.Order;
import com.springboot.jenka_coffee.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/orders")
public class ApiAdminOrderController {

    private final OrderService orderService;

    public ApiAdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrders(
            @RequestParam("page") Optional<Integer> page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page.orElse(0), size, Sort.by("createDate").descending());
        Page<Order> orderPage = orderService.findAll(pageable);

        Map<String, Object> data = new HashMap<>();
        data.put("items", orderPage.getContent());
        data.put("currentPage", orderPage.getNumber());
        data.put("totalPages", orderPage.getTotalPages());
        data.put("totalItems", orderPage.getTotalElements());

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn hàng thành công", data));
    }

    @PutMapping("/{id}/status/{status}")
    public ResponseEntity<ApiResponse<Void>> updateOrderStatus(
            @PathVariable("id") Long id,
            @PathVariable("status") int status) {
        try {
            orderService.updateStatus(id, status);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái đơn hàng thành công", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi cập nhật trạng thái đơn hàng: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable("id") Long id) {
        try {
            // Thay vì xóa, ta chuyển sang trạng thái Hủy (4) theo logic cũ
            orderService.updateStatus(id, 4);
            return ResponseEntity.ok(ApiResponse.success("Đã hủy đơn hàng", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi hủy đơn hàng: " + e.getMessage()));
        }
    }
}
