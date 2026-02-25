package com.springboot.jenka_coffee.controller.admin;

import org.springframework.stereotype.Controller;

import com.springboot.jenka_coffee.service.ReportService;
import com.springboot.jenka_coffee.service.OrderService;
import com.springboot.jenka_coffee.repository.ProductRepository;
import com.springboot.jenka_coffee.repository.AccountRepository;
import com.springboot.jenka_coffee.repository.OrderRepository;
import com.springboot.jenka_coffee.dto.response.OrderStatsDTO;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.math.BigDecimal;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final ReportService reportService;
    private final OrderService orderService;
    private final ProductRepository productRepository;
    private final AccountRepository accountRepository;
    private final OrderRepository orderRepository;

    public AdminDashboardController(ReportService reportService,
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

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        OrderStatsDTO stats = reportService.getOrderStats();

        long totalOrders = orderRepository.count();
        long totalProducts = productRepository.count();
        long totalCustomers = accountRepository.count();

        model.addAttribute("totalRevenue", stats.getTotalRevenue() != null ? stats.getTotalRevenue() : BigDecimal.ZERO);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("totalCustomers", totalCustomers);

        // Mock new growth indicators for now
        model.addAttribute("newOrdersToday", 0);
        model.addAttribute("newProducts", 0);
        model.addAttribute("newCustomers", 0);
        model.addAttribute("revenueGrowth", 0);

        // Recent 5 orders
        model.addAttribute("recentOrders",
                orderService.findAll(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createDate"))).getContent());

        return "admin/dashboard";
    }
}
