package com.springboot.jenka_coffee.api.admin;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.entity.Order;
import com.springboot.jenka_coffee.entity.OrderDetail;
import com.springboot.jenka_coffee.repository.OrderRepository;
import com.springboot.jenka_coffee.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrderDetail(@PathVariable Long id) {
        Order order = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng #" + id));
        return ResponseEntity.ok(ApiResponse.success("OK", toDetailDto(order)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createDate").descending());
        Page<Order> orderPage = orderService.findAll(pageable);

        List<Long> ids = orderPage.getContent().stream().map(Order::getId).collect(Collectors.toList());
        List<Order> orders = orderService.findAllWithAccountByIds(ids);
        orders.sort((a, b) -> b.getCreateDate().compareTo(a.getCreateDate()));

        Map<String, Object> data = new HashMap<>();
        data.put("items", orders.stream().map(this::toDto).collect(Collectors.toList()));
        data.put("currentPage", orderPage.getNumber());
        data.put("totalPages", orderPage.getTotalPages());
        data.put("totalItems", orderPage.getTotalElements());

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn hàng thành công", data));
    }

    @PutMapping("/{id}/status/{status}")
    public ResponseEntity<ApiResponse<Void>> updateOrderStatus(
            @PathVariable Long id,
            @PathVariable int status) {
        orderService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable Long id) {
        orderService.updateStatus(id, Order.OrderStatus.CANCELLED.getValue());
        return ResponseEntity.ok(ApiResponse.success("Đã hủy đơn hàng", null));
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
