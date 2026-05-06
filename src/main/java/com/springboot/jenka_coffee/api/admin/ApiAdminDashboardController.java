package com.springboot.jenka_coffee.api.admin;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.dto.response.DashboardCountsDTO;
import com.springboot.jenka_coffee.dto.response.OrderStatsDTO;
import com.springboot.jenka_coffee.dto.response.RevenueReportDTO;
import com.springboot.jenka_coffee.dto.response.TopProductDTO;
import com.springboot.jenka_coffee.entity.Order;
import com.springboot.jenka_coffee.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/dashboard")
public class ApiAdminDashboardController {

    private final ReportService reportService;

    public ApiAdminDashboardController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        OrderStatsDTO stats = reportService.getOrderStats();
        DashboardCountsDTO counts = reportService.getDashboardCounts();

        int currentYear = LocalDateTime.now().getYear();
        List<RevenueReportDTO> monthlyRevenue = reportService.getMonthlyRevenue(currentYear);
        
        // Fill missing months with 0 revenue
        Map<Integer, BigDecimal> revenueByMonth = monthlyRevenue.stream()
                .collect(java.util.stream.Collectors.toMap(
                        r -> r.getMonth() != null ? r.getMonth() : 0,
                        r -> r.getTotalRevenue() != null ? r.getTotalRevenue() : BigDecimal.ZERO
                ));
        
        List<Map<String, Object>> chartData = new java.util.ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("month", m);
            entry.put("revenue", revenueByMonth.getOrDefault(m, BigDecimal.ZERO));
            chartData.add(entry);
        }

        List<Order> recentOrders = reportService.getRecentOrders(5);
        List<Map<String, Object>> orderDTOs = recentOrders.stream()
                .sorted(java.util.Comparator.comparing(Order::getCreateDate, 
                        java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .map(o -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", o.getId());
            dto.put("createDate", o.getCreateDate() != null ? o.getCreateDate().toString() : null);
            dto.put("totalAmount", o.getTotalAmount());
            dto.put("status", o.getStatus());
            if (o.getAccount() != null) {
                dto.put("account", Map.of(
                    "fullname", o.getAccount().getFullname(),
                    "phone",    o.getAccount().getPhone() != null ? o.getAccount().getPhone() : ""
                ));
            } else {
                dto.put("account", null);
            }
            return dto;
        }).collect(Collectors.toList());

        List<TopProductDTO> topProducts = reportService.getTopProducts(5);

        Map<String, Object> data = new HashMap<>();
        data.put("totalRevenue",   stats.getTotalRevenue() != null ? stats.getTotalRevenue() : BigDecimal.ZERO);
        data.put("totalOrders",    counts.totalOrders());
        data.put("totalProducts",  counts.totalProducts());
        data.put("totalCustomers", counts.totalCustomers());
        data.put("recentOrders",   orderDTOs);
        data.put("monthlyRevenue", chartData);
        data.put("topProducts",    topProducts);

        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin dashboard thành công", data));
    }

    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRevenue(
            @RequestParam(defaultValue = "month") String period,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer value) {

        int y = year != null ? year : LocalDateTime.now().getYear();
        LocalDateTime from, to;

        switch (period) {
            case "week" -> {
                int week = value != null ? value : LocalDateTime.now().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                from = java.time.LocalDate.of(y, 1, 1)
                        .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, (long) week)
                        .with(DayOfWeek.MONDAY)
                        .atStartOfDay();
                to   = from.plusDays(7).minusSeconds(1);
            }
            case "quarter" -> {
                int q = value != null ? value : (LocalDateTime.now().getMonthValue() - 1) / 3 + 1;
                int startMonth = (q - 1) * 3 + 1;
                from = LocalDateTime.of(y, startMonth, 1, 0, 0);
                to   = from.plusMonths(3).minusSeconds(1);
            }
            case "year" -> {
                from = LocalDateTime.of(y, 1, 1, 0, 0);
                to   = LocalDateTime.of(y, 12, 31, 23, 59, 59);
            }
            default -> { // month
                int m = value != null ? value : LocalDateTime.now().getMonthValue();
                from = LocalDateTime.of(y, m, 1, 0, 0);
                to   = from.plusMonths(1).minusSeconds(1);
            }
        }

        List<RevenueReportDTO> revenueData = reportService.getRevenueByDateRange(from, to);
        List<TopProductDTO> topProducts = reportService.getTopProducts(10);

        Map<String, Object> result = new HashMap<>();
        result.put("data", revenueData);
        result.put("topProducts", topProducts);
        result.put("from", from.toString());
        result.put("to", to.toString());

        return ResponseEntity.ok(ApiResponse.success("OK", result));
    }
}

