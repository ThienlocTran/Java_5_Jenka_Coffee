package com.springboot.jenka_coffee.api.admin;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.entity.Order;
import com.springboot.jenka_coffee.exception.BusinessRuleException;
import com.springboot.jenka_coffee.exception.ResourceNotFoundException;
import com.springboot.jenka_coffee.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin Order Controller - Clean 3-Tier
 * Controller: HTTP only, no Repository injection, no business logic
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class ApiAdminOrderController {

    private final OrderService orderService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrderDetail(@PathVariable Long id) {
        try {
            Order order = orderService.findByIdWithDetails(id);
            return ResponseEntity.ok(ApiResponse.success("OK", toDetailDto(order)));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(ApiResponse.error("Không tìm thấy đơn hàng #" + id));
        } catch (Exception e) {
            log.error("Error getting order detail: {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Lỗi khi lấy thông tin đơn hàng"));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            // Validate pagination
            size = Math.min(Math.max(size, 1), 100);
            page = Math.max(page, 0);

            Pageable pageable = PageRequest.of(page, size, Sort.by("createDate").descending());
            Page<Order> orderPage = orderService.findAll(pageable);

            List<Long> ids = orderPage.getContent().stream().map(Order::getId).collect(Collectors.toList());
            // TC-ORD-CTRL-005 FIX: Wrap in ArrayList before sort() to avoid UnsupportedOperationException
            // findAllWithAccountByIds([]) returns List.of() (immutable) when ids is empty
            List<Order> orders = new java.util.ArrayList<>(orderService.findAllWithAccountByIds(ids));
            orders.sort((a, b) -> b.getCreateDate().compareTo(a.getCreateDate()));

            Map<String, Object> data = new HashMap<>();
            data.put("items", orders.stream().map(this::toDto).collect(Collectors.toList()));
            data.put("currentPage", orderPage.getNumber());
            data.put("totalPages", orderPage.getTotalPages());
            data.put("totalItems", orderPage.getTotalElements());

            return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn hàng thành công", data));
        } catch (Exception e) {
            log.error("Error getting orders", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Lỗi khi lấy danh sách đơn hàng"));
        }
    }

    @PutMapping("/{id}/status/{status}")
    public ResponseEntity<ApiResponse<Void>> updateOrderStatus(
            @PathVariable Long id,
            @PathVariable int status) {
        // Validate status range (0: NEW, 1: CONFIRMED, 2: CANCELLED)
        if (status < 0 || status > 2) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Trạng thái đơn hàng không hợp lệ (0: Mới, 1: Đã xác nhận, 2: Đã hủy)"));
        }
        try {
            orderService.updateStatus(id, status);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công", null));
        } catch (ResourceNotFoundException e) {
            // TC-ORD-CTRL-012 FIX: Order not found → 404
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (BusinessRuleException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating order status: {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Lỗi khi cập nhật trạng thái"));
        }
    }

    /**
     * Cancel order - POST /cancel (REST semantics)
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable Long id) {
        try {
            orderService.updateStatus(id, Order.OrderStatus.CANCELLED.getValue());
            return ResponseEntity.ok(ApiResponse.success("Đã hủy đơn hàng", null));
        } catch (ResourceNotFoundException e) {
            // TC-ORD-CTRL-016 FIX: Per CSV spec, return 400 (not 404) for cancel endpoint
            // Note: This differs from updateStatus endpoint which returns 404
            // Rationale: Cancel is a business operation, not-found is treated as bad request
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (BusinessRuleException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error cancelling order: {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Lỗi khi hủy đơn hàng"));
        }
    }

    /** Backward compatible with frontend */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> cancelOrderLegacy(@PathVariable Long id) {
        return cancelOrder(id);
    }

    // ========== DTO MAPPING (TODO: Move to separate mapper class) ==========
    private Map<String, Object> toDto(Order o) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", o.getId());
        dto.put("address", o.getAddress());
        dto.put("phone", o.getPhone());
        dto.put("status", o.getStatus());
        dto.put("totalAmount", o.getTotalAmount());
        
        // Format createDate as ISO-8601 string for frontend
        if (o.getCreateDate() != null) {
            // Use DateTimeFormatter to ensure consistent format
            java.time.format.DateTimeFormatter formatter = 
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            dto.put("createDate", o.getCreateDate().format(formatter));
        } else {
            dto.put("createDate", null);
        }

        if (o.getAccount() != null) {
            dto.put("account", Map.of(
                "username", o.getAccount().getUsername(),
                "fullname", o.getAccount().getFullname(),
                "phone",    o.getAccount().getPhone() != null ? o.getAccount().getPhone() : ""
            ));
        } else {
            dto.put("account", null);
        }
        return dto;
    }

    /** Detail DTO — includes order items with product info */
    private Map<String, Object> toDetailDto(Order o) {
        Map<String, Object> dto = toDto(o);

        List<Map<String, Object>> items = o.getOrderDetails() == null ? List.of() :
            o.getOrderDetails().stream().map(d -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", d.getId());
                item.put("quantity", d.getQuantity());
                item.put("price", d.getPrice());
                if (d.getProduct() != null) {
                    item.put("productId", d.getProduct().getId());
                    item.put("productName", d.getProduct().getName());
                    item.put("productImage", d.getProduct().getImage());
                }
                return item;
            }).collect(Collectors.toList());

        dto.put("orderDetails", items);
        return dto;
    }
}
