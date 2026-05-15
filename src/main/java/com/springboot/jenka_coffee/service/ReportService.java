package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.dto.response.DashboardCountsDTO;
import com.springboot.jenka_coffee.dto.response.OrderStatsDTO;
import com.springboot.jenka_coffee.dto.response.RevenueReportDTO;
import com.springboot.jenka_coffee.dto.response.TopCustomerDTO;
import com.springboot.jenka_coffee.dto.response.TopProductDTO;
import com.springboot.jenka_coffee.entity.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface ReportService {

    List<RevenueReportDTO> getMonthlyRevenue(int year);

    List<RevenueReportDTO> getYearlyRevenue();

    List<RevenueReportDTO> getRevenueByDateRange(LocalDateTime from, LocalDateTime to);

    List<TopCustomerDTO> getTopCustomers(int limit);

    List<TopProductDTO> getTopProducts(int limit);

    OrderStatsDTO getOrderStats();

    DashboardCountsDTO getDashboardCounts();

    List<Order> getRecentOrders(int limit);

    List<Order> getOrdersForRevenueReport(LocalDateTime from, LocalDateTime to);

    BigDecimal getTotalRevenueBetween(LocalDateTime from, LocalDateTime to);

    long countOrdersBetween(LocalDateTime from, LocalDateTime to);

    long countProductsBetween(LocalDateTime from, LocalDateTime to);

    long countCustomersBetween(LocalDateTime from, LocalDateTime to);
}
