package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.dto.response.OrderStatsDTO;
import com.springboot.jenka_coffee.dto.response.RevenueReportDTO;
import com.springboot.jenka_coffee.dto.response.TopCustomerDTO;

import java.util.List;

/**
 * Service for analytics and reporting
 * Provides revenue, customer, and order statistics
 */
public interface ReportService {

    /**
     * Get monthly revenue for a specific year
     */
    List<RevenueReportDTO> getMonthlyRevenue(int year);

    /**
     * Get yearly revenue aggregation
     */
    List<RevenueReportDTO> getYearlyRevenue();

    /**
     * Get top customers by total spending
     * 
     * @param limit Number of customers to return
     */
    List<TopCustomerDTO> getTopCustomers(int limit);

    /**
     * Get overall order statistics
     * Total orders, revenue, and average order value
     */
    OrderStatsDTO getOrderStats();
}
