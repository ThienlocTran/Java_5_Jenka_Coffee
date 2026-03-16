package com.springboot.jenka_coffee.api.admin;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.dto.response.OrderStatsDTO;
import com.springboot.jenka_coffee.dto.response.RevenueReportDTO;
import com.springboot.jenka_coffee.entity.Order;
import com.springboot.jenka_coffee.repository.AccountRepository;
import com.springboot.jenka_coffee.repository.OrderRepository;
import com.springboot.jenka_coffee.repository.ProductRepository;
import com.springboot.jenka_coffee.service.ReportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/dashboard")
public class ApiAdminDashboardController {

    private final ReportService reportService;
    private final ProductRepository productRepository;
    private final AccountRepository accountRepository;
    private final OrderRepository orderRepository;

    public ApiAdminDashboardController(ReportService reportService,
            ProductRepository productRepository,
            AccountRepository accountRepository,
            OrderRepository orderRepository) {
        this.reportService = reportService;
        this.productRepository = productRepository;
        this.accountRepository = accountRepository;
        this.orderRepository = orderRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        OrderStatsDTO stats = reportService.getOrderStats();

        long totalOrders = orderRepository.count();
        long totalProducts = productRepository.count();
        long totalCustomers = accountRepository.count();

        // Recent 5 orders — fetch IDs first, then JOIN FETCH account
        Page<Order> recentPage = orderRepository.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createDate")));
        List<Long> ids = recentPage.getContent().stream().map(Order::getId).collect(Collectors.toList());
        List<Order> recentOrders = ids.isEmpty() ? List.of() : orderRepository.findAllWithAccountByIds(ids);
        // Re-sort by createDate desc after JOIN FETCH
        recentOrders.sort((a, b) -> b.getCreateDate().compareTo(a.getCreateDate()));

        // Monthly revenue for current year chart
        int currentYear = LocalDateTime.now().getYear();
        List<RevenueReportDTO> monthlyRevenue = orderRepository.getMonthlyRevenue(currentYear);
        List<Map<String, Object>> chartData = monthlyRevenue.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("month", r.getMonth() != null ? r.getMonth().intValue() : 0);
            m.put("revenue", r.getTotalRevenue() != null ? r.getTotalRevenue() : BigDecimal.ZERO);
            return m;
        }).collect(Collectors.toList());

        // Build response DTO for each order (avoid lazy proxy issues)
        List<Map<String, Object>> orderDTOs = recentOrders.stream().map(o -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", o.getId());
            dto.put("createDate", o.getCreateDate() != null ? o.getCreateDate().toString() : null);
            dto.put("totalAmount", o.getTotalAmount());
            dto.put("status", o.getStatus());
            if (o.getAccount() != null) {
                Map<String, Object> acc = new HashMap<>();
                acc.put("fullname", o.getAccount().getFullname());
                acc.put("phone", o.getAccount().getPhone());
                dto.put("account", acc);
            } else {
                dto.put("account", null);
            }
            return dto;
        }).collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("totalRevenue", stats.getTotalRevenue() != null ? stats.getTotalRevenue() : BigDecimal.ZERO);
        data.put("totalOrders", totalOrders);
        data.put("totalProducts", totalProducts);
        data.put("totalCustomers", totalCustomers);
        data.put("newOrdersToday", 0);
        data.put("newProducts", 0);
        data.put("newCustomers", 0);
        data.put("revenueGrowth", 0);
        data.put("recentOrders", orderDTOs);
        data.put("monthlyRevenue", chartData);

        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin dashboard thành công", data));
    }
}
