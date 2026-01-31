package com.springboot.jenka_coffee.controller.admin;

import com.springboot.jenka_coffee.dto.response.OrderStatsDTO;
import com.springboot.jenka_coffee.dto.response.RevenueReportDTO;
import com.springboot.jenka_coffee.dto.response.TopCustomerDTO;
import com.springboot.jenka_coffee.service.ReportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Year;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/report")
public class AdminReportController {

    private final ReportService reportService;

    public AdminReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/revenue")
    public String revenue(Model model) {
        // --- 1. KHỞI TẠO LIST RỖNG
        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();
        List<Object[]> tableData = new ArrayList<>();

        // --- 2. GỬI SANG VIEW
        model.addAttribute("chartLabels", labels);
        model.addAttribute("chartData", data);
        model.addAttribute("tableData", tableData);

        // Tổng tiền mặc định là 0
        model.addAttribute("totalRevenue", 0);

        return "admin/reports/revenue-report";
    }

    @GetMapping("/customer")
    public String vipCustomer(Model model) {
        // 1. Tạo list rỗng (Code sạch)
        List<Object[]> vipList = new ArrayList<>();

        // 2. Gửi sang View
        model.addAttribute("vipList", vipList);

        return "admin/reports/vip-customer";
    }

    // ===== REST API ENDPOINTS (JSON) =====

    /**
     * Get monthly revenue for a specific year
     */
    @GetMapping("/api/revenue/monthly")
    @ResponseBody
    public List<RevenueReportDTO> getMonthlyRevenue(
            @RequestParam(value = "year", defaultValue = "0") int year) {
        if (year == 0) {
            year = Year.now().getValue();
        }
        return reportService.getMonthlyRevenue(year);
    }

    /**
     * Get yearly revenue aggregation
     */
    @GetMapping("/api/revenue/yearly")
    @ResponseBody
    public List<RevenueReportDTO> getYearlyRevenue() {
        return reportService.getYearlyRevenue();
    }

    /**
     * Get top customers by total spending
     */
    @GetMapping("/api/customers/top")
    @ResponseBody
    public List<TopCustomerDTO> getTopCustomers(
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return reportService.getTopCustomers(limit);
    }

    /**
     * Get overall order statistics
     */
    @GetMapping("/api/stats/overview")
    @ResponseBody
    public OrderStatsDTO getOrderStats() {
        return reportService.getOrderStats();
    }
}