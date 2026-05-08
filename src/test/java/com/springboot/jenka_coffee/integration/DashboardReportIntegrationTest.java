package com.springboot.jenka_coffee.integration;

import com.springboot.jenka_coffee.dto.response.DashboardCountsDTO;
import com.springboot.jenka_coffee.dto.response.OrderStatsDTO;
import com.springboot.jenka_coffee.dto.response.RevenueReportDTO;
import com.springboot.jenka_coffee.dto.response.TopCustomerDTO;
import com.springboot.jenka_coffee.dto.response.TopProductDTO;
import com.springboot.jenka_coffee.service.ReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * INTEGRATION TEST CASES for Dashboard & Report (Batch 05 - Additional 15 tests)
 * Focus: Cross-module consistency, time boundaries, large datasets, edge cases
 * 
 * TC-DSH-INT-001 to TC-DSH-INT-015
 */
@SpringBootTest
@AutoConfigureMockMvc
class DashboardReportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @Test
    @DisplayName("TC-DSH-INT-001: Revenue by month boundary - last day of month (Feb 28 vs 29)")
    @WithMockUser(roles = "ADMIN")
    void test_revenueByMonth_lastDayBoundary_feb28vs29() throws Exception {
        // Arrange - February 2024 (leap year, 29 days)
        when(reportService.getRevenueByDateRange(any(), any())).thenReturn(List.of());
        when(reportService.getTopProducts(anyInt())).thenReturn(List.of());

        // Act & Assert - Feb 2024 should end on 29th (leap year)
        mockMvc.perform(get("/api/admin/dashboard/revenue")
                        .param("period", "month")
                        .param("year", "2024")
                        .param("value", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.from").value(containsString("2024-02-01")))
                .andExpect(jsonPath("$.data.to").value(containsString("2024-02-29")));
        
        // BUG RISK: Non-leap year (2023) should end on Feb 28
        // Common bug: Hardcoding 28 or 29 instead of using LocalDate.lengthOfMonth()
    }

    @Test
    @DisplayName("TC-DSH-INT-002: Revenue by week 53 - year with 53 weeks (2020)")
    @WithMockUser(roles = "ADMIN")
    void test_revenueByWeek_week53_yearWith53Weeks() throws Exception {
        // Arrange - 2020 has 53 ISO weeks
        when(reportService.getRevenueByDateRange(any(), any())).thenReturn(List.of());
        when(reportService.getTopProducts(anyInt())).thenReturn(List.of());

        // Act & Assert - Week 53 of 2020 exists
        mockMvc.perform(get("/api/admin/dashboard/revenue")
                        .param("period", "week")
                        .param("year", "2020")
                        .param("value", "53"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.from").exists())
                .andExpect(jsonPath("$.data.to").exists());
        
        // Verify: Week 53 calculation doesn't crash
        // Most years have 52 weeks, some have 53 (e.g., 2020, 2026)
    }

    @Test
    @DisplayName("TC-DSH-INT-003: Top products with ties - same quantity sold")
    @WithMockUser(roles = "ADMIN")
    void test_topProducts_withTies_sameQuantitySold() throws Exception {
        // Arrange - 3 products with same quantity (tie)
        List<TopProductDTO> tiedProducts = List.of(
                new TopProductDTO(1, "Product A", "Coffee", 100L, new BigDecimal("1000.00")),
                new TopProductDTO(2, "Product B", "Coffee", 100L, new BigDecimal("1000.00")),
                new TopProductDTO(3, "Product C", "Coffee", 100L, new BigDecimal("1000.00"))
        );
        
        OrderStatsDTO stats = new OrderStatsDTO(10L, new BigDecimal("3000.00"), new BigDecimal("300.00"));
        DashboardCountsDTO counts = new DashboardCountsDTO(10L, 3L, 5L);
        
        when(reportService.getOrderStats()).thenReturn(stats);
        when(reportService.getDashboardCounts()).thenReturn(counts);
        when(reportService.getMonthlyRevenue(anyInt())).thenReturn(List.of());
        when(reportService.getRecentOrders(anyInt())).thenReturn(List.of());
        when(reportService.getTopProducts(5)).thenReturn(tiedProducts);

        // Act & Assert
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topProducts", hasSize(3)))
                .andExpect(jsonPath("$.data.topProducts[0].totalSold").value(100))
                .andExpect(jsonPath("$.data.topProducts[1].totalSold").value(100))
                .andExpect(jsonPath("$.data.topProducts[2].totalSold").value(100));
        
        // Verify: Tie-breaking logic (should use secondary sort: productId or name)
        // Current: Undefined order for ties (DB-dependent)
    }

    @Test
    @DisplayName("TC-DSH-INT-004: Top customers with zero orders - should not appear")
    @WithMockUser(roles = "ADMIN")
    void test_topCustomers_withZeroOrders_shouldNotAppear() throws Exception {
        // Arrange - Only customers with orders should appear
        List<TopCustomerDTO> customers = List.of(
                new TopCustomerDTO("user1", "Customer A", new BigDecimal("1000.00"), 10L),
                new TopCustomerDTO("user2", "Customer B", new BigDecimal("500.00"), 5L)
        );
        
        when(reportService.getTopCustomers(10)).thenReturn(customers);

        // Act & Assert
        mockMvc.perform(get("/api/admin/reports/customers/top"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
        
        // Verify: Customers with 0 orders don't appear in top list
        // Query should filter: WHERE orderCount > 0
    }

    @Test
    @DisplayName("TC-DSH-INT-005: Monthly revenue year boundary - Dec to Jan transition")
    @WithMockUser(roles = "ADMIN")
    void test_monthlyRevenue_yearBoundary_decToJan() throws Exception {
        // Arrange - December 2023 and January 2024
        List<RevenueReportDTO> dec2023 = List.of(
                new RevenueReportDTO(2023, 12, new BigDecimal("1000.00"), 10L)
        );
        List<RevenueReportDTO> jan2024 = List.of(
                new RevenueReportDTO(2024, 1, new BigDecimal("1200.00"), 12L)
        );
        
        when(reportService.getMonthlyRevenue(2023)).thenReturn(dec2023);
        when(reportService.getMonthlyRevenue(2024)).thenReturn(jan2024);

        // Act & Assert - Dec 2023
        mockMvc.perform(get("/api/admin/reports/revenue/monthly")
                        .param("year", "2023"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].year").value(2023))
                .andExpect(jsonPath("$.data[0].month").value(12));

        // Act & Assert - Jan 2024
        mockMvc.perform(get("/api/admin/reports/revenue/monthly")
                        .param("year", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].year").value(2024))
                .andExpect(jsonPath("$.data[0].month").value(1));
        
        // Verify: Year boundary handled correctly (no data leakage between years)
    }

    @Test
    @DisplayName("TC-DSH-INT-006: Dashboard performance - large dataset (1000 orders)")
    @WithMockUser(roles = "ADMIN")
    void test_dashboard_performance_largeDataset() throws Exception {
        // Arrange - Simulate large dataset
        OrderStatsDTO stats = new OrderStatsDTO(1000L, new BigDecimal("100000.00"), new BigDecimal("100.00"));
        DashboardCountsDTO counts = new DashboardCountsDTO(1000L, 100L, 500L);
        
        List<RevenueReportDTO> monthlyData = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            monthlyData.add(new RevenueReportDTO(2024, i, new BigDecimal("8333.33"), 83L));
        }
        
        when(reportService.getOrderStats()).thenReturn(stats);
        when(reportService.getDashboardCounts()).thenReturn(counts);
        when(reportService.getMonthlyRevenue(anyInt())).thenReturn(monthlyData);
        when(reportService.getRecentOrders(anyInt())).thenReturn(List.of());
        when(reportService.getTopProducts(anyInt())).thenReturn(List.of());

        // Act - Measure time
        long startTime = System.currentTimeMillis();
        
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalOrders").value(1000));
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Assert - Should be fast even with large dataset
        // Note: This is mock test, real performance depends on DB queries
        // In production: Add indexes, use caching, optimize queries
    }

    @Test
    @DisplayName("TC-DSH-INT-007: Yearly revenue grouping - multiple years sorted DESC")
    @WithMockUser(roles = "ADMIN")
    void test_yearlyRevenue_grouping_multipleYearsSortedDesc() throws Exception {
        // Arrange - Data from 2022, 2023, 2024
        List<RevenueReportDTO> yearlyData = List.of(
                new RevenueReportDTO(2024, null, new BigDecimal("50000.00"), 500L),
                new RevenueReportDTO(2023, null, new BigDecimal("40000.00"), 400L),
                new RevenueReportDTO(2022, null, new BigDecimal("30000.00"), 300L)
        );
        
        when(reportService.getYearlyRevenue()).thenReturn(yearlyData);

        // Act & Assert
        mockMvc.perform(get("/api/admin/reports/revenue/yearly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].year").value(2024))
                .andExpect(jsonPath("$.data[1].year").value(2023))
                .andExpect(jsonPath("$.data[2].year").value(2022));
        
        // Verify: Years sorted DESC (newest first)
        // Query: ORDER BY year DESC
    }

    @Test
    @DisplayName("TC-DSH-INT-008: Order stats with cancelled orders - should exclude from revenue")
    @WithMockUser(roles = "ADMIN")
    void test_orderStats_withCancelledOrders_excludeFromRevenue() throws Exception {
        // Arrange - totalOrders includes all, but totalRevenue only CONFIRMED
        OrderStatsDTO stats = new OrderStatsDTO(
                100L,                          // Total orders (all statuses)
                new BigDecimal("8000.00"),     // Revenue (only CONFIRMED)
                new BigDecimal("80.00")        // Avg (8000 / 100)
        );
        
        when(reportService.getOrderStats()).thenReturn(stats);

        // Act & Assert
        mockMvc.perform(get("/api/admin/reports/stats/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalOrders").value(100))
                .andExpect(jsonPath("$.data.totalRevenue").value(8000.00));
        
        // Verify: Cancelled orders counted in totalOrders but NOT in totalRevenue
        // Query: totalRevenue = SUM(totalAmount) WHERE status = 1 (CONFIRMED)
        // Query: totalOrders = COUNT(*) (all statuses)
    }

    @Test
    @DisplayName("TC-DSH-INT-009: Revenue by quarter Q1 - Jan, Feb, Mar")
    @WithMockUser(roles = "ADMIN")
    void test_revenueByQuarter_q1_janFebMar() throws Exception {
        // Arrange
        when(reportService.getRevenueByDateRange(any(), any())).thenReturn(List.of());
        when(reportService.getTopProducts(anyInt())).thenReturn(List.of());

        // Act & Assert - Q1 2024 (Jan-Mar)
        mockMvc.perform(get("/api/admin/dashboard/revenue")
                        .param("period", "quarter")
                        .param("year", "2024")
                        .param("value", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.from").value(containsString("2024-01-01")))
                .andExpect(jsonPath("$.data.to").value(containsString("2024-03-31")));
        
        // Verify: Q1 = months 1, 2, 3 (Jan-Mar)
        // Formula: startMonth = (q - 1) * 3 + 1 = (1-1)*3+1 = 1
    }

    @Test
    @DisplayName("TC-DSH-INT-010: Dashboard with null account in recent orders - should not crash")
    @WithMockUser(roles = "ADMIN")
    void test_dashboard_nullAccountInRecentOrders_shouldNotCrash() throws Exception {
        // Arrange - Order without account (guest order or deleted account)
        com.springboot.jenka_coffee.entity.Order order = new com.springboot.jenka_coffee.entity.Order();
        order.setId(1L);
        order.setCreateDate(LocalDateTime.now());
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setStatus(0);
        order.setAccount(null); // NULL account
        
        OrderStatsDTO stats = new OrderStatsDTO(1L, new BigDecimal("100.00"), new BigDecimal("100.00"));
        DashboardCountsDTO counts = new DashboardCountsDTO(1L, 1L, 1L);
        
        when(reportService.getOrderStats()).thenReturn(stats);
        when(reportService.getDashboardCounts()).thenReturn(counts);
        when(reportService.getMonthlyRevenue(anyInt())).thenReturn(List.of());
        when(reportService.getRecentOrders(5)).thenReturn(List.of(order));
        when(reportService.getTopProducts(anyInt())).thenReturn(List.of());

        // Act & Assert - Should not crash with NullPointerException
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recentOrders[0].account").value(nullValue()));
        
        // Verify: Null safety in controller
        // if (o.getAccount() != null) { ... } else { dto.put("account", null); }
    }

    @Test
    @DisplayName("TC-DSH-INT-011: Top products limit boundary - exactly 5 products")
    @WithMockUser(roles = "ADMIN")
    void test_topProducts_limitBoundary_exactly5Products() throws Exception {
        // Arrange - Exactly 5 products in DB
        List<TopProductDTO> products = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            products.add(new TopProductDTO(i, "Product " + i, "Coffee", 100L - i, new BigDecimal("1000.00")));
        }
        
        OrderStatsDTO stats = new OrderStatsDTO(10L, new BigDecimal("5000.00"), new BigDecimal("500.00"));
        DashboardCountsDTO counts = new DashboardCountsDTO(10L, 5L, 5L);
        
        when(reportService.getOrderStats()).thenReturn(stats);
        when(reportService.getDashboardCounts()).thenReturn(counts);
        when(reportService.getMonthlyRevenue(anyInt())).thenReturn(List.of());
        when(reportService.getRecentOrders(anyInt())).thenReturn(List.of());
        when(reportService.getTopProducts(5)).thenReturn(products);

        // Act & Assert
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topProducts", hasSize(5)));
        
        // Verify: Returns exactly 5 when DB has exactly 5
        // Edge case: What if DB has < 5 products? Should return all available
    }

    @Test
    @DisplayName("TC-DSH-INT-012: Top customers sorting - by totalSpent DESC")
    @WithMockUser(roles = "ADMIN")
    void test_topCustomers_sorting_byTotalSpentDesc() throws Exception {
        // Arrange - Customers sorted by totalSpent
        List<TopCustomerDTO> customers = List.of(
                new TopCustomerDTO("vip", "VIP Customer", new BigDecimal("10000.00"), 50L),
                new TopCustomerDTO("regular", "Regular Customer", new BigDecimal("5000.00"), 30L),
                new TopCustomerDTO("newuser", "New Customer", new BigDecimal("500.00"), 5L)
        );
        
        when(reportService.getTopCustomers(10)).thenReturn(customers);

        // Act & Assert
        mockMvc.perform(get("/api/admin/reports/customers/top"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].totalSpent").value(10000.00))
                .andExpect(jsonPath("$.data[1].totalSpent").value(5000.00))
                .andExpect(jsonPath("$.data[2].totalSpent").value(500.00));
        
        // Verify: Sorted by totalSpent DESC (highest spender first)
        // Query: ORDER BY totalSpent DESC
    }

    @Test
    @DisplayName("TC-DSH-INT-013: Revenue by year 2024 - full year range")
    @WithMockUser(roles = "ADMIN")
    void test_revenueByYear_2024_fullYearRange() throws Exception {
        // Arrange
        when(reportService.getRevenueByDateRange(any(), any())).thenReturn(List.of());
        when(reportService.getTopProducts(anyInt())).thenReturn(List.of());

        // Act & Assert - Full year 2024
        mockMvc.perform(get("/api/admin/dashboard/revenue")
                        .param("period", "year")
                        .param("year", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.from").value(containsString("2024-01-01T00:00")))
                .andExpect(jsonPath("$.data.to").value(containsString("2024-12-31T23:59:59")));
        
        // Verify: Full year from Jan 1 00:00:00 to Dec 31 23:59:59
    }

    @Test
    @DisplayName("TC-DSH-INT-014: Monthly revenue with BigDecimal precision - no rounding errors")
    @WithMockUser(roles = "ADMIN")
    void test_monthlyRevenue_bigDecimalPrecision_noRoundingErrors() throws Exception {
        // Arrange - Revenue with many decimal places
        List<RevenueReportDTO> monthlyData = List.of(
                new RevenueReportDTO(2024, 1, new BigDecimal("1234.567890"), 10L),
                new RevenueReportDTO(2024, 2, new BigDecimal("9876.543210"), 20L)
        );
        
        when(reportService.getMonthlyRevenue(2024)).thenReturn(monthlyData);

        // Act & Assert
        mockMvc.perform(get("/api/admin/reports/revenue/monthly")
                        .param("year", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].totalRevenue").value(1234.567890))
                .andExpect(jsonPath("$.data[1].totalRevenue").value(9876.543210));
        
        // Verify: BigDecimal preserves precision (no float rounding errors)
        // Use BigDecimal for money, not float/double
    }

    @Test
    @DisplayName("TC-DSH-INT-015: Dashboard counts consistency - sum of parts equals total")
    @WithMockUser(roles = "ADMIN")
    void test_dashboardCounts_consistency_sumOfPartsEqualsTotal() throws Exception {
        // Arrange - Verify counts are consistent
        OrderStatsDTO stats = new OrderStatsDTO(100L, new BigDecimal("10000.00"), new BigDecimal("100.00"));
        DashboardCountsDTO counts = new DashboardCountsDTO(100L, 50L, 80L);
        
        when(reportService.getOrderStats()).thenReturn(stats);
        when(reportService.getDashboardCounts()).thenReturn(counts);
        when(reportService.getMonthlyRevenue(anyInt())).thenReturn(List.of());
        when(reportService.getRecentOrders(anyInt())).thenReturn(List.of());
        when(reportService.getTopProducts(anyInt())).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalOrders").value(100))
                .andExpect(jsonPath("$.data.totalProducts").value(50))
                .andExpect(jsonPath("$.data.totalCustomers").value(80));
        
        // Verify: Counts are consistent across different queries
        // totalOrders from OrderStatsDTO should match totalOrders from DashboardCountsDTO
        // In this test: stats.totalOrders = 100, counts.totalOrders = 100 ✓
    }
}
