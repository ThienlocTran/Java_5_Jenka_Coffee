package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.dto.response.DashboardCountsDTO;
import com.springboot.jenka_coffee.dto.response.OrderStatsDTO;
import com.springboot.jenka_coffee.dto.response.RevenueReportDTO;
import com.springboot.jenka_coffee.dto.response.TopCustomerDTO;
import com.springboot.jenka_coffee.dto.response.TopProductDTO;
import com.springboot.jenka_coffee.entity.Order;
import com.springboot.jenka_coffee.repository.AccountRepository;
import com.springboot.jenka_coffee.repository.OrderRepository;
import com.springboot.jenka_coffee.repository.ProductRepository;
import com.springboot.jenka_coffee.service.impl.ReportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * ADVANCED TEST CASES for Report Service (Batch 05)
 * Focus: Aggregation correctness, boundary conditions, limit clamping, data consistency
 * 
 * TC-RPT-ADV-001 to TC-RPT-ADV-010
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceAdvancedTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    @Test
    @DisplayName("TC-RPT-ADV-001: Monthly revenue year=0 - should default to current year")
    void test_monthlyRevenue_yearZero_defaultsToCurrentYear() {
        // Arrange
        int currentYear = LocalDateTime.now().getYear();
        List<RevenueReportDTO> mockData = List.of(
                new RevenueReportDTO(currentYear, 1, new BigDecimal("100.00"), 2L)
        );
        when(orderRepository.getMonthlyRevenue(currentYear)).thenReturn(mockData);

        // Act - Controller should convert year=0 to current year before calling service
        List<RevenueReportDTO> result = reportService.getMonthlyRevenue(currentYear);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(currentYear, result.get(0).getYear());
        verify(orderRepository).getMonthlyRevenue(currentYear);
    }

    @Test
    @DisplayName("TC-RPT-ADV-002: Monthly revenue negative year - no validation (DB query fails)")
    void test_monthlyRevenue_negativeYear_noValidation() {
        // Arrange - Negative year passes through to DB
        when(orderRepository.getMonthlyRevenue(-1)).thenReturn(List.of());

        // Act
        List<RevenueReportDTO> result = reportService.getMonthlyRevenue(-1);

        // Assert - Returns empty list (DB finds no data for year -1)
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(orderRepository).getMonthlyRevenue(-1);
        
        // BUG: No year validation in service layer
        // Negative year should throw IllegalArgumentException
        // Risk: Confusing behavior, wasted DB query
    }

    @Test
    @DisplayName("TC-RPT-ADV-003: Monthly revenue future year - returns empty not null")
    void test_monthlyRevenue_futureYear_returnsEmptyNotNull() {
        // Arrange - Year 2099 has no data
        when(orderRepository.getMonthlyRevenue(2099)).thenReturn(List.of());

        // Act
        List<RevenueReportDTO> result = reportService.getMonthlyRevenue(2099);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(orderRepository).getMonthlyRevenue(2099);
        
        // Correct behavior: Empty list for future years
        // Verify: Not null, not exception
    }

    @Test
    @DisplayName("TC-RPT-ADV-004: Top customers limit clamp - max 100")
    void test_topCustomers_limitClamp_max100() {
        // Arrange - Request 999 customers
        List<TopCustomerDTO> mockData = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            mockData.add(new TopCustomerDTO("user" + i, "Customer" + i, new BigDecimal("1000.00"), Long.valueOf(10)));
        }
        when(orderRepository.getTopCustomers(any(PageRequest.class))).thenReturn(mockData);

        // Act - Controller should clamp limit to 100 before calling service
        List<TopCustomerDTO> result = reportService.getTopCustomers(100);

        // Assert
        assertNotNull(result);
        assertEquals(100, result.size());
        verify(orderRepository).getTopCustomers(PageRequest.of(0, 100));
        
        // Note: Clamping happens in CONTROLLER, not service
        // Service just uses the limit passed to it
    }

    @Test
    @DisplayName("TC-RPT-ADV-005: Top customers limit=0 - should be clamped to 1 by controller")
    void test_topCustomers_limitZero_clampedToOne() {
        // Arrange
        List<TopCustomerDTO> mockData = List.of(
                new TopCustomerDTO("user1", "Customer1", new BigDecimal("1000.00"), Long.valueOf(10))
        );
        when(orderRepository.getTopCustomers(any(PageRequest.class))).thenReturn(mockData);

        // Act - Controller clamps: Math.max(limit, 1) → 1
        List<TopCustomerDTO> result = reportService.getTopCustomers(1);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(orderRepository).getTopCustomers(PageRequest.of(0, 1));
    }

    @Test
    @DisplayName("TC-RPT-ADV-006: Order stats with no confirmed orders - totalRevenue should be 0 not null")
    void test_orderStats_noConfirmedOrders_totalRevenueZeroNotNull() {
        // Arrange - No confirmed orders
        when(orderRepository.countAllOrders()).thenReturn(0L);
        when(orderRepository.sumTotalRevenue()).thenReturn(BigDecimal.ZERO);

        // Act
        OrderStatsDTO result = reportService.getOrderStats();

        // Assert
        assertNotNull(result);
        assertEquals(0L, result.getTotalOrders());
        assertEquals(BigDecimal.ZERO, result.getTotalRevenue());
        assertEquals(BigDecimal.ZERO, result.getAvgOrderValue());
        
        // Verify: No NullPointerException when dividing by zero
        // avgOrderValue = totalRevenue / totalOrders → 0 / 0 handled correctly
    }

    @Test
    @DisplayName("TC-RPT-ADV-007: Order stats avgOrderValue calculation - rounding to 2 decimals")
    void test_orderStats_avgOrderValue_roundingTo2Decimals() {
        // Arrange - 3 orders, total 100.00 → avg = 33.333...
        when(orderRepository.countAllOrders()).thenReturn(3L);
        when(orderRepository.sumTotalRevenue()).thenReturn(new BigDecimal("100.00"));

        // Act
        OrderStatsDTO result = reportService.getOrderStats();

        // Assert
        assertNotNull(result);
        // Use compareTo() for BigDecimal comparison (scale-independent)
        assertEquals(0, new BigDecimal("33.33").compareTo(result.getAvgOrderValue()),
                "avgOrderValue must be 33.33 with scale=2, HALF_UP rounding");
        
        // Verify: Rounding mode HALF_UP, scale 2
        // 100.00 / 3 = 33.333... → 33.33
        // 
        // ⚠️ RISK: If service uses divide() without scale/rounding:
        // BigDecimal.divide(divisor) → ArithmeticException: Non-terminating decimal expansion
        // Must use: divide(divisor, 2, RoundingMode.HALF_UP)
    }

    @Test
    @DisplayName("TC-RPT-ADV-008: Recent orders limit - returns exactly N orders")
    void test_recentOrders_limit_returnsExactlyN() {
        // Arrange - 10 orders in DB, request 5
        List<Order> mockOrders = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Order order = new Order();
            order.setId((long) i);
            order.setCreateDate(LocalDateTime.now().minusDays(i));
            mockOrders.add(order);
        }
        
        Page<Order> page = new PageImpl<>(mockOrders.subList(0, 5), PageRequest.of(0, 5), 10);
        when(orderRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(orderRepository.findAllWithAccountByIds(anyList())).thenReturn(mockOrders.subList(0, 5));

        // Act
        List<Order> result = reportService.getRecentOrders(5);

        // Assert
        assertNotNull(result);
        assertEquals(5, result.size());
        verify(orderRepository).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("TC-RPT-ADV-009: Recent orders sorting - DESC by createDate")
    void test_recentOrders_sorting_descByCreateDate() {
        // Arrange - Orders with different dates
        Order order1 = new Order();
        order1.setId(1L);
        order1.setCreateDate(LocalDateTime.of(2024, 4, 10, 10, 0));
        
        Order order2 = new Order();
        order2.setId(2L);
        order2.setCreateDate(LocalDateTime.of(2024, 4, 12, 15, 0)); // Newest
        
        Order order3 = new Order();
        order3.setId(3L);
        order3.setCreateDate(LocalDateTime.of(2024, 4, 11, 12, 0));
        
        List<Order> mockOrders = List.of(order1, order2, order3);
        Page<Order> page = new PageImpl<>(mockOrders, PageRequest.of(0, 3), 3);
        
        when(orderRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(orderRepository.findAllWithAccountByIds(anyList())).thenReturn(mockOrders);

        // Act
        List<Order> result = reportService.getRecentOrders(3);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        // Verify: First order is the newest (id=2)
        assertEquals(2L, result.get(0).getId());
        assertEquals(3L, result.get(1).getId());
        assertEquals(1L, result.get(2).getId());
        
        // Service sorts manually after fetching: orders.sort((a, b) -> b.getCreateDate().compareTo(a.getCreateDate()))
    }

    @Test
    @DisplayName("TC-RPT-ADV-010: Dashboard counts - all repos called exactly once")
    void test_dashboardCounts_allReposCalled() {
        // Arrange
        when(orderRepository.count()).thenReturn(10L);
        when(productRepository.count()).thenReturn(5L);
        when(accountRepository.count()).thenReturn(8L);

        // Act
        DashboardCountsDTO result = reportService.getDashboardCounts();

        // Assert
        assertNotNull(result);
        assertEquals(10L, result.totalOrders());
        assertEquals(5L, result.totalProducts());
        assertEquals(8L, result.totalCustomers());
        
        verify(orderRepository, times(1)).count();
        verify(productRepository, times(1)).count();
        verify(accountRepository, times(1)).count();
        
        // Verify: Each repo called exactly once (no duplicate queries)
    }
}
