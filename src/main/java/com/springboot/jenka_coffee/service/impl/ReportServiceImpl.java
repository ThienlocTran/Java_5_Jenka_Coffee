package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.dto.response.DashboardCountsDTO;
import com.springboot.jenka_coffee.dto.response.OrderStatsDTO;
import com.springboot.jenka_coffee.dto.response.RevenueReportDTO;
import com.springboot.jenka_coffee.dto.response.TopCustomerDTO;
import com.springboot.jenka_coffee.dto.response.TopProductDTO;
import com.springboot.jenka_coffee.entity.Order;
import com.springboot.jenka_coffee.repository.AccountRepository;
import com.springboot.jenka_coffee.repository.OrderRepository;
import com.springboot.jenka_coffee.repository.ProductRepository;
import com.springboot.jenka_coffee.service.ReportService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final AccountRepository accountRepository;

    public ReportServiceImpl(OrderRepository orderRepository,
                             ProductRepository productRepository,
                             AccountRepository accountRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.accountRepository = accountRepository;
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
    public List<TopProductDTO> getTopProducts(int limit) {
        return orderRepository.getTopProducts(PageRequest.of(0, limit));
    }

    @Override
    public List<RevenueReportDTO> getRevenueByDateRange(LocalDateTime from, LocalDateTime to) {
        return orderRepository.getRevenueByDateRange(from, to);
    }

    /**
     * VULN-DASHBOARD-PERFORMANCE WARNING: This method performs full table scans
     * Current implementation is acceptable for small datasets (<10k orders)
     * For production with >100k orders, consider:
     * 1. Add @Cacheable with TTL (e.g., 5 minutes)
     * 2. Use materialized views or summary tables
     * 3. Implement incremental counters updated on order creation
     * 4. Add database indexes on frequently queried columns
     */
    @Override
    public OrderStatsDTO getOrderStats() {
        long totalOrders = orderRepository.countAllOrders();
        BigDecimal totalRevenue = orderRepository.sumTotalRevenue();
        BigDecimal avgOrderValue = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        return new OrderStatsDTO(totalOrders, totalRevenue, avgOrderValue);
    }

    @Override
    public DashboardCountsDTO getDashboardCounts() {
        return new DashboardCountsDTO(
                orderRepository.count(),
                productRepository.count(),
                accountRepository.count()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getRecentOrders(int limit) {
        var page = orderRepository.findAll(
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createDate")));
        List<Long> ids = page.getContent().stream().map(Order::getId).collect(Collectors.toList());
        if (ids.isEmpty()) return List.of();
        List<Order> orders = new java.util.ArrayList<>(orderRepository.findAllWithAccountByIds(ids));
        orders.sort((a, b) -> b.getCreateDate().compareTo(a.getCreateDate()));
        return orders;
    }
}
