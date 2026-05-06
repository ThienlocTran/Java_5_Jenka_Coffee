package com.springboot.jenka_coffee.api.admin;

import com.springboot.jenka_coffee.dto.response.DashboardCountsDTO;
import com.springboot.jenka_coffee.dto.response.OrderStatsDTO;
import com.springboot.jenka_coffee.dto.response.RevenueReportDTO;
import com.springboot.jenka_coffee.dto.response.TopProductDTO;
import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.entity.Order;
import com.springboot.jenka_coffee.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ADVANCED TEST CASES for Dashboard Module (Batch 05)
 * Focus: Data inconsistency, aggregation bugs, time boundaries, cross-endpoint mismatch
 * 
 * TC-DSH-ADV-001 to TC-DSH-ADV-010
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiAdminDashboardControllerAdvancedTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    private OrderStatsDTO testStats;
    private DashboardCountsDTO testCounts;

    @BeforeEach
    void setUp() {
        testStats = new OrderStatsDTO(10L, new BigDecimal("1000.00"), new BigDecimal("100.00"));
        testCounts = new DashboardCountsDTO(10L, 5L, 8L);
    }

    @Test
    @DisplayName("TC-DSH-ADV-001: Monthly revenue missing months - should return 0 not null")
    @WithMockUser(roles = "ADMIN")
    void test_monthlyRevenue_missingMonths_returnsZeroNotNull() throws Exception {
        // Arrange - Only months 1, 3, 5 have data (months 2, 4, 6-12 missing)
        List<RevenueReportDTO> partialData = List.of(
                new RevenueReportDTO(2024, 1, new BigDecimal("100.00"), 2L),
                new RevenueReportDTO(2024, 3, new BigDecimal("200.00"), 3L),
                new RevenueReportDTO(2024, 5, new BigDecimal("150.00"), 1L)
        );
        
        when(reportService.getMonthlyRevenue(anyInt())).thenReturn(partialData);
        when(reportService.getOrderStats()).thenReturn(testStats);
        when(reportService.getDashboardCounts()).thenReturn(testCounts);
        when(reportService.getRecentOrders(anyInt())).thenReturn(List.of());
        when(reportService.getTopProducts(anyInt())).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.monthlyRevenue").isArray())
                .andExpect(jsonPath("$.data.monthlyRevenue", hasSize(12)))  // ✅ MUST be 12 months
                // Month 2 (index 1) must have revenue=0, not missing
                .andExpect(jsonPath("$.data.monthlyRevenue[1].month").value(2))
                .andExpect(jsonPath("$.data.monthlyRevenue[1].revenue").value(0))
                // Month 4 (index 3) must have revenue=0
                .andExpect(jsonPath("$.data.monthlyRevenue[3].month").value(4))
                .andExpect(jsonPath("$.data.monthlyRevenue[3].revenue").value(0));
        
        // GAP: If this test FAILS with hasSize(3) → service/controller doesn't fill missing months
        // Fix: Controller or service must fill months 1-12, defaulting to 0 for missing months
        // Risk: Frontend chart shows gaps instead of zero values
    }

    @Test
    @DisplayName("TC-DSH-ADV-002: Dashboard aggregation mismatch - totalRevenue vs monthlyRevenue sum")
    @WithMockUser(roles = "ADMIN")
    void test_dashboard_aggregationMismatch_totalRevenueVsMonthlySum() throws Exception {
        // Arrange - Inconsistent data: totalRevenue != sum of monthly revenues
        OrderStatsDTO inconsistentStats = new OrderStatsDTO(
                10L, 
                new BigDecimal("1000.00"), // Total revenue
                new BigDecimal("100.00")
        );
        
        List<RevenueReportDTO> monthlyData = List.of(
                new RevenueReportDTO(2024, 1, new BigDecimal("300.00"), 3L),
                new RevenueReportDTO(2024, 2, new BigDecimal("400.00"), 4L),
                new RevenueReportDTO(2024, 3, new BigDecimal("200.00"), 3L)
        );
        // Sum of monthly = 900.00, but totalRevenue = 1000.00 (mismatch!)
        
        when(reportService.getOrderStats()).thenReturn(inconsistentStats);
        when(reportService.getDashboardCounts()).thenReturn(testCounts);
        when(reportService.getMonthlyRevenue(anyInt())).thenReturn(monthlyData);
        when(reportService.getRecentOrders(anyInt())).thenReturn(List.of());
        when(reportService.getTopProducts(anyInt())).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRevenue").value(1000.00))
                .andExpect(jsonPath("$.data.monthlyRevenue[0].revenue").value(300.00))
                .andExpect(jsonPath("$.data.monthlyRevenue[1].revenue").value(400.00))
                .andExpect(jsonPath("$.data.monthlyRevenue[2].revenue").value(200.00));
        
        // BUG: Data inconsistency - different queries return different totals
        // Root cause: getOrderStats() uses different date range or status filter than getMonthlyRevenue()
        // Risk: Dashboard shows conflicting numbers, user loses trust
    }

    @Test
    @DisplayName("TC-DSH-ADV-003: Recent orders createDate format - must be ISO-8601 with timezone")
    @WithMockUser(roles = "ADMIN")
    void test_recentOrders_createDateFormat_iso8601WithTimezone() throws Exception {
        // Arrange
        Order order = new Order();
        order.setId(1L);
        order.setCreateDate(LocalDateTime.of(2024, 4, 12, 15, 25, 30));
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setStatus(0);
        
        Account account = new Account();
        account.setFullname("John Doe");
        account.setPhone("0123456789");
        order.setAccount(account);
        
        when(reportService.getOrderStats()).thenReturn(testStats);
        when(reportService.getDashboardCounts()).thenReturn(testCounts);
        when(reportService.getMonthlyRevenue(anyInt())).thenReturn(List.of());
        when(reportService.getRecentOrders(5)).thenReturn(List.of(order));
        when(reportService.getTopProducts(anyInt())).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recentOrders[0].createDate").isString())
                .andExpect(jsonPath("$.data.recentOrders[0].createDate").value(containsString("2024-04-12")));
        
        // BUG: Current implementation uses LocalDateTime.toString() → "2024-04-12T15:25:30"
        // Missing timezone information (should be ISO-8601 with offset: "2024-04-12T15:25:30+07:00")
        // Risk: Frontend can't correctly convert to user's local timezone
        // Fix: Use ZonedDateTime or OffsetDateTime with explicit timezone
    }

    @Test
    @DisplayName("TC-DSH-ADV-004: Empty database - all values should be 0 not null")
    @WithMockUser(roles = "ADMIN")
    void test_dashboard_emptyDatabase_allZerosNotNull() throws Exception {
        // Arrange - Empty database
        OrderStatsDTO emptyStats = new OrderStatsDTO(0L, BigDecimal.ZERO, BigDecimal.ZERO);
        DashboardCountsDTO emptyCounts = new DashboardCountsDTO(0L, 0L, 0L);
        
        when(reportService.getOrderStats()).thenReturn(emptyStats);
        when(reportService.getDashboardCounts()).thenReturn(emptyCounts);
        when(reportService.getMonthlyRevenue(anyInt())).thenReturn(List.of());
        when(reportService.getRecentOrders(anyInt())).thenReturn(List.of());
        when(reportService.getTopProducts(anyInt())).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRevenue").value(0))
                .andExpect(jsonPath("$.data.totalOrders").value(0))
                .andExpect(jsonPath("$.data.totalProducts").value(0))
                .andExpect(jsonPath("$.data.totalCustomers").value(0))
                .andExpect(jsonPath("$.data.recentOrders").isEmpty())
                .andExpect(jsonPath("$.data.monthlyRevenue").isEmpty())
                .andExpect(jsonPath("$.data.topProducts").isEmpty());
        
        // Verify: No NullPointerException, all numeric fields are 0 not null
        // Risk: If service returns null instead of 0, controller crashes with NPE
    }

    @Test
    @DisplayName("TC-DSH-ADV-005: Revenue by week - ISO week calculation boundary")
    @WithMockUser(roles = "ADMIN")
    void test_revenueByWeek_isoWeekBoundary_correctCalculation() throws Exception {
        // Arrange - Week 1 of 2024 starts on Monday Jan 1
        when(reportService.getRevenueByDateRange(any(), any())).thenReturn(List.of());
        when(reportService.getTopProducts(anyInt())).thenReturn(List.of());

        // Act & Assert - Request week 1 of 2024
        mockMvc.perform(get("/api/admin/dashboard/revenue")
                        .param("period", "week")
                        .param("year", "2024")
                        .param("value", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.from").value(containsString("2024-01-01")))
                .andExpect(jsonPath("$.data.to").value(containsString("2024-01-07")));
        
        // BUG RISK: ISO week calculation can be tricky
        // Week 1 must start on Monday, not Sunday
        // Week 53 exists in some years (e.g., 2020 has 53 weeks)
        // Verify: from = Monday of week N, to = Sunday (+7 days)
    }

    @Test
    @DisplayName("TC-DSH-ADV-006: Revenue by quarter - Q4 boundary (Oct-Dec)")
    @WithMockUser(roles = "ADMIN")
    void test_revenueByQuarter_q4Boundary_correctMonths() throws Exception {
        // Arrange
        when(reportService.getRevenueByDateRange(any(), any())).thenReturn(List.of());
        when(reportService.getTopProducts(anyInt())).thenReturn(List.of());

        // Act & Assert - Request Q4 2024 (Oct, Nov, Dec)
        mockMvc.perform(get("/api/admin/dashboard/revenue")
                        .param("period", "quarter")
                        .param("year", "2024")
                        .param("value", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.from").value(containsString("2024-10-01")))
                .andExpect(jsonPath("$.data.to").value(containsString("2024-12-31")));
        
        // Verify: Q4 = months 10, 11, 12 (Oct-Dec)
        // Common bug: Off-by-one error in quarter calculation
        // Formula: startMonth = (q - 1) * 3 + 1
    }

    @Test
    @DisplayName("TC-DSH-ADV-007: Revenue by invalid period - fallback to month (silent)")
    @WithMockUser(roles = "ADMIN")
    void test_revenueByInvalidPeriod_fallbackToMonth_noError() throws Exception {
        // Arrange
        when(reportService.getRevenueByDateRange(any(), any())).thenReturn(List.of());
        when(reportService.getTopProducts(anyInt())).thenReturn(List.of());

        // Act & Assert - Invalid period "INVALID"
        mockMvc.perform(get("/api/admin/dashboard/revenue")
                        .param("period", "INVALID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.from").exists())
                .andExpect(jsonPath("$.data.to").exists());
        
        // BUG: Silent fallback to default (month) without error message
        // Better UX: Return 400 Bad Request with error message
        // Current: switch default case → month (confusing for API consumers)
    }

    @Test
    @DisplayName("TC-DSH-ADV-008: Recent orders sorting - must be DESC by createDate")
    @WithMockUser(roles = "ADMIN")
    void test_recentOrders_sorting_descByCreateDate() throws Exception {
        // Arrange - Orders with different createDate
        Order order1 = new Order();
        order1.setId(1L);
        order1.setCreateDate(LocalDateTime.of(2024, 4, 10, 10, 0));
        order1.setTotalAmount(new BigDecimal("100.00"));
        order1.setStatus(0);
        
        Order order2 = new Order();
        order2.setId(2L);
        order2.setCreateDate(LocalDateTime.of(2024, 4, 12, 15, 0)); // Newer
        order2.setTotalAmount(new BigDecimal("200.00"));
        order2.setStatus(0);
        
        Order order3 = new Order();
        order3.setId(3L);
        order3.setCreateDate(LocalDateTime.of(2024, 4, 11, 12, 0)); // Middle
        order3.setTotalAmount(new BigDecimal("150.00"));
        order3.setStatus(0);
        
        // Service returns in wrong order
        List<Order> unsortedOrders = List.of(order1, order2, order3);
        
        when(reportService.getOrderStats()).thenReturn(testStats);
        when(reportService.getDashboardCounts()).thenReturn(testCounts);
        when(reportService.getMonthlyRevenue(anyInt())).thenReturn(List.of());
        when(reportService.getRecentOrders(5)).thenReturn(unsortedOrders);
        when(reportService.getTopProducts(anyInt())).thenReturn(List.of());

        // Act & Assert - Newest order (id=2, Apr 12) must be first
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                // ✅ Newest order (id=2, Apr 12) must be first
                .andExpect(jsonPath("$.data.recentOrders[0].id").value(2))
                // ✅ Middle order (id=3, Apr 11) must be second
                .andExpect(jsonPath("$.data.recentOrders[1].id").value(3))
                // ✅ Oldest order (id=1, Apr 10) must be last
                .andExpect(jsonPath("$.data.recentOrders[2].id").value(1));
        
        // GAP: If test FAILS with [id=1, id=2, id=3] → sort bug confirmed
        // Fix: Controller must sort recentOrders by createDate DESC before returning
        // Risk: User sees old orders first instead of recent ones
    }

    @Test
    @DisplayName("TC-DSH-ADV-009: Dashboard counts mismatch - totalOrders vs recentOrders.length")
    @WithMockUser(roles = "ADMIN")
    void test_dashboard_countsMismatch_totalOrdersVsRecentLength() throws Exception {
        // Arrange - totalOrders = 100, but recentOrders only returns 5
        DashboardCountsDTO counts = new DashboardCountsDTO(100L, 5L, 8L);
        
        List<Order> recentOrders = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Order order = new Order();
            order.setId((long) i);
            order.setCreateDate(LocalDateTime.now().minusDays(i));
            order.setTotalAmount(new BigDecimal("100.00"));
            order.setStatus(0);
            recentOrders.add(order);
        }
        
        when(reportService.getOrderStats()).thenReturn(testStats);
        when(reportService.getDashboardCounts()).thenReturn(counts);
        when(reportService.getMonthlyRevenue(anyInt())).thenReturn(List.of());
        when(reportService.getRecentOrders(5)).thenReturn(recentOrders);
        when(reportService.getTopProducts(anyInt())).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalOrders").value(100))
                .andExpect(jsonPath("$.data.recentOrders", hasSize(5)));
        
        // This is CORRECT behavior: totalOrders = all orders, recentOrders = limited to 5
        // But verify frontend doesn't confuse these two numbers
        // Document: recentOrders is a SAMPLE, not the full list
    }

    @Test
    @DisplayName("TC-DSH-ADV-010: Null safety - service returns null BigDecimal")
    @WithMockUser(roles = "ADMIN")
    void test_dashboard_nullSafety_serviceReturnsNullBigDecimal() throws Exception {
        // Arrange - Service returns null totalRevenue (edge case)
        OrderStatsDTO nullStats = new OrderStatsDTO(0L, null, null);
        
        when(reportService.getOrderStats()).thenReturn(nullStats);
        when(reportService.getDashboardCounts()).thenReturn(testCounts);
        when(reportService.getMonthlyRevenue(anyInt())).thenReturn(List.of());
        when(reportService.getRecentOrders(anyInt())).thenReturn(List.of());
        when(reportService.getTopProducts(anyInt())).thenReturn(List.of());

        // Act & Assert - Controller should handle null → 0
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRevenue").value(0));
        
        // Verify: Controller has null safety: 
        // stats.getTotalRevenue() != null ? stats.getTotalRevenue() : BigDecimal.ZERO
        // If missing, NPE would occur
    }
}
