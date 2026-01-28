package com.springboot.jenka_coffee.controller.admin;

import org.springframework.stereotype.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    @GetMapping("/dashboard")
    public String dashboard() {
        // TODO: Thêm logic lấy thống kê từ service
        return "admin/dashboard";
    }
}
