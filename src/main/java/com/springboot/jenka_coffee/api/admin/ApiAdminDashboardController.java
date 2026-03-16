package com.springboot.jenka_coffee.api.admin;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.dto.response.OrderStatsDTO;
import com.springboot.jenka_coffee.repository.AccountRepository;
import com.springboot.jenka_coffee.repository.OrderRepository;
import com.springboot.jenka_coffee.repository.ProductRepository;
import com.springboot.jenka_coffee.service.OrderService;
import com.springboot.jenka_coffee.service.ReportService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
public class ApiAdminDashboardController {

    private final ReportService reportService;
    private final OrderService orderService;
    private final ProductRepository productRepository;
    private final AccountRepository accountRepository;
    private final OrderRepository orderRepository;

    public ApiAdminDashboardController(ReportService reportService,
            OrderService orderService,
            ProductRepository productRepository,
            AccountRepository accountRepository,
            OrderRepository orderRepository) {
        this.reportService = reportService;
        this.orderService = orderService;
        this.productRepository = productRepository;
        this.accountRepository = accountRepository;
        this.orderRepository = orderRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        OrderStatsDTO stats = reportService.getOrderStats();

        long totalOrders = orderRepository.count();
        long totalProducts = productRepository.count();
        long totalCustomers = accountRepository.count();

        Map<String, Object> data = new HashMap<>();
        data.put("totalRevenue", stats.getTotalRevenue() != null ? stats.getTotalRevenue() : BigDecimal.ZERO);
        data.put("totalOrders", totalOrders);
        data.put("totalProducts", totalProducts);
        data.put("totalCustomers", totalCustomers);

        // Mock new growth indicators for now
        data.put("newOrdersToday", 0);
        data.put("newProducts", 0);
        data.put("newCustomers", 0);
        data.put("revenueGrowth", 0);

        // Recent 5 orders
        data.put("recentOrders",
                orderService.findAll(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createDate"))).getContent());

        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin dashboard thành công", data));
    }
}
