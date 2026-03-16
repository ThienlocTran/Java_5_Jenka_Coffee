package com.springboot.jenka_coffee.api.admin;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.entity.Order;
import com.springboot.jenka_coffee.repository.OrderRepository;
import com.springboot.jenka_coffee.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/orders")
public class ApiAdminOrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    public ApiAdminOrderController(OrderService orderService, OrderRepository orderRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrders(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createDate").descending());
        Page<Order> orderPage = orderService.findAll(pageable);

        // JOIN FETCH account to avoid lazy proxy serialization
        List<Long> ids = orderPage.getContent().stream().map(Order::getId).collect(Collectors.toList());
        List<Order> orders = ids.isEmpty() ? List.of() : orderRepository.findAllWithAccountByIds(ids);
        orders.sort((a, b) -> b.getCreateDate().compareTo(a.getCreateDate()));

        List<Map<String, Object>> dtos = orders.stream().map(this::toDto).collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("items", dtos);
        data.put("currentPage", orderPage.getNumber());
        data.put("totalPages", orderPage.getTotalPages());
        data.put("totalItems", orderPage.getTotalElements());

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn hàng thành công", data));
    }

    @PutMapping("/{id}/status/{status}")
    public ResponseEntity<ApiResponse<Void>> updateOrderStatus(
            @PathVariable Long id,
            @PathVariable int status) {
        try {
            orderService.updateStatus(id, status);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi cập nhật trạng thái: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable Long id) {
        try {
            orderService.updateStatus(id, 4); // 4 = Cancelled
            return ResponseEntity.ok(ApiResponse.success("Đã hủy đơn hàng", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi hủy đơn hàng: " + e.getMessage()));
        }
    }

    /** Safe DTO — only scalar fields + account basics, no lazy proxies */
    private Map<String, Object> toDto(Order o) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", o.getId());
        dto.put("address", o.getAddress());
        dto.put("phone", o.getPhone());
        dto.put("status", o.getStatus());
        dto.put("totalAmount", o.getTotalAmount());
        dto.put("createDate", o.getCreateDate() != null ? o.getCreateDate().toString() : null);

        if (o.getAccount() != null) {
            Map<String, Object> acc = new HashMap<>();
            acc.put("username", o.getAccount().getUsername());
            acc.put("fullname", o.getAccount().getFullname());
            acc.put("phone", o.getAccount().getPhone());
            dto.put("account", acc);
        } else {
            dto.put("account", null);
        }
        return dto;
    }
}
