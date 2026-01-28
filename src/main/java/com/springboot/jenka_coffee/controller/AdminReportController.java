package com.springboot.jenka_coffee.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/report")
public class AdminReportController {

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
}