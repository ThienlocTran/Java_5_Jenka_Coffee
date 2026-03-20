package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.dto.response.DashboardCountsDTO;
import com.springboot.jenka_coffee.dto.response.OrderStatsDTO;
import com.springboot.jenka_coffee.dto.response.RevenueReportDTO;
import com.springboot.jenka_coffee.dto.response.TopCustomerDTO;
import com.springboot.jenka_coffee.entity.Order;

import java.util.List;

public interface ReportService {

    List<RevenueReportDTO> getMonthlyRevenue(int year);

    List<RevenueReportDTO> getYearlyRevenue();

    List<TopCustomerDTO> getTopCustomers(int limit);

    OrderStatsDTO getOrderStats();

    /** Aggregate counts for dashboard summary cards (orders, products, customers) */
    DashboardCountsDTO getDashboardCounts();

    /** Recent N orders with account eagerly loaded — avoids lazy proxy in controller */
    List<Order> getRecentOrders(int limit);
}
