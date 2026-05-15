package com.springboot.jenka_coffee.api.admin;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.dto.response.OrderStatsDTO;
import com.springboot.jenka_coffee.dto.response.RevenueReportDTO;
import com.springboot.jenka_coffee.dto.response.TopCustomerDTO;
import com.springboot.jenka_coffee.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Year;
import java.util.List;

@RestController
@RequestMapping("/api/admin/reports")
public class ApiAdminReportController {

    private final ReportService reportService;

    public ApiAdminReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/revenue/monthly")
    public ResponseEntity<ApiResponse<List<RevenueReportDTO>>> getMonthlyRevenue(
            @RequestParam(value = "year", defaultValue = "0") int year) {

        if (year == 0) {
            year = Year.now().getValue();
        }

        List<RevenueReportDTO> report = reportService.getMonthlyRevenue(year);
        return ResponseEntity.ok(ApiResponse.success("Lấy báo cáo doanh thu tháng thành công", report));
    }

    @GetMapping("/revenue/yearly")
    public ResponseEntity<ApiResponse<List<RevenueReportDTO>>> getYearlyRevenue() {
        List<RevenueReportDTO> report = reportService.getYearlyRevenue();
        return ResponseEntity.ok(ApiResponse.success("Lấy báo cáo doanh thu năm thành công", report));
    }

    @GetMapping("/customers/top")
    public ResponseEntity<ApiResponse<List<TopCustomerDTO>>> getTopCustomers(
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        // Giới hạn cứng tránh query toàn bộ DB
        limit = Math.min(Math.max(limit, 1), 100);
        List<TopCustomerDTO> report = reportService.getTopCustomers(limit);
        return ResponseEntity.ok(ApiResponse.success("Lấy báo cáo khách hàng VIP thành công", report));
    }

    @GetMapping("/stats/overview")
    public ResponseEntity<ApiResponse<OrderStatsDTO>> getOrderStatsOverview() {
        OrderStatsDTO stats = reportService.getOrderStats();
        return ResponseEntity.ok(ApiResponse.success("Lấy thống kê tổng quan thành công", stats));
    }
}
