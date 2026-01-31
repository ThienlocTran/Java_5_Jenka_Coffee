package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.dto.response.OrderStatsDTO;
import com.springboot.jenka_coffee.dto.response.RevenueReportDTO;
import com.springboot.jenka_coffee.dto.response.TopCustomerDTO;
import com.springboot.jenka_coffee.entity.Order;
import com.springboot.jenka_coffee.repository.OrderRepository;
import com.springboot.jenka_coffee.service.ReportService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    private final OrderRepository orderRepository;

    public ReportServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public List<RevenueReportDTO> getMonthlyRevenue(int year) {
        return orderRepository.getMonthlyRevenue(year);
    }

    @Override
    public List<RevenueReportDTO> getYearlyRevenue() {
        return orderRepository.getYearlyRevenue();
    }

    @Override
    public List<TopCustomerDTO> getTopCustomers(int limit) {
        return orderRepository.getTopCustomers(PageRequest.of(0, limit));
    }

    @Override
    public OrderStatsDTO getOrderStats() {
        List<Order> allOrders = orderRepository.findAll();

        long totalOrders = allOrders.size();
        BigDecimal totalRevenue = allOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgOrderValue = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new OrderStatsDTO(totalOrders, totalRevenue, avgOrderValue);
    }
}
