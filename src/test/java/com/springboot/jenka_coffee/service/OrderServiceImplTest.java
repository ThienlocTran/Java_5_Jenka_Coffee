package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.entity.Order;
import com.springboot.jenka_coffee.exception.BusinessRuleException;
import com.springboot.jenka_coffee.exception.ResourceNotFoundException;
import com.springboot.jenka_coffee.repository.OrderRepository;
import com.springboot.jenka_coffee.repository.PointHistoryRepository;
import com.springboot.jenka_coffee.service.impl.OrderServiceImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ORDER SERVICE TEST CASES (Batch 02)
 * TC-ORD-SER-001 to TC-ORD-SER-003
 * 
 * Focus: Service layer business logic, exception handling
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartService cartService;

    @Mock
    private EmailService emailService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order testOrder;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setUsername("testuser");
        testAccount.setFullname("Test User");
        testAccount.setEmail("test@example.com");
        testAccount.setPhone("0123456789");
        testAccount.setPoints(100);

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setAccount(testAccount);
        testOrder.setAddress("123 Test St");
        testOrder.setPhone("0123456789");
        testOrder.setStatus(0); // NEW
        testOrder.setTotalAmount(new BigDecimal("500000"));
        testOrder.setCreateDate(LocalDateTime.now());
        testOrder.setPointsUsed(0);
    }

    @Test
    @DisplayName("TC-ORD-SER-001: Order Service - Update status on non-existent order")
    void test_updateStatus_orderNotFound_throwsException() {
        // Arrange - Mock EntityManager to return null (order not found)
        when(entityManager.find(eq(Order.class), eq(99999L), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.updateStatus(99999L, 1);
        });

        assertTrue(exception.getMessage().contains("Không tìm thấy đơn hàng"));
    }

    @Test
    @DisplayName("TC-ORD-SER-002: Order Service - Cancel already confirmed order")
    void test_updateStatus_cancelConfirmedOrder_throwsBusinessRuleException() {
        // Arrange - Order is CONFIRMED (status=1)
        testOrder.setStatus(1);
        when(entityManager.find(eq(Order.class), eq(testOrder.getId()), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(testOrder);

        // Act & Assert - Try to cancel (status=2)
        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> {
            orderService.updateStatus(testOrder.getId(), 2);
        });

        assertTrue(exception.getMessage().contains("Không thể chuyển trạng thái"));
        assertTrue(exception.getMessage().contains("CONFIRMED"));
        assertTrue(exception.getMessage().contains("CANCELLED"));
    }

    @Test
    @DisplayName("TC-ORD-SER-003: Order Service - FindById with details not found")
    void test_findByIdWithDetails_orderNotFound_throwsResourceNotFoundException() {
        // Arrange - Mock repository to return empty
        when(orderRepository.findByIdWithDetails(99999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            orderService.findByIdWithDetails(99999L);
        });

        assertTrue(exception.getMessage().contains("Không tìm thấy đơn hàng"));
        assertTrue(exception.getMessage().contains("99999"));
    }

    @Test
    @DisplayName("TC-ORD-SER-004: Order Service - Update status NEW to CONFIRMED (valid transition)")
    void test_updateStatus_newToConfirmed_success() {
        // Arrange - Order is NEW (status=0)
        testOrder.setStatus(0);
        when(entityManager.find(eq(Order.class), eq(testOrder.getId()), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(testOrder);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        assertDoesNotThrow(() -> {
            orderService.updateStatus(testOrder.getId(), 1);
        });

        // Assert
        verify(orderRepository, times(1)).save(testOrder);
        assertEquals(1, testOrder.getStatus());
    }

    @Test
    @DisplayName("TC-ORD-SER-005: Order Service - Update status NEW to CANCELLED (valid transition)")
    void test_updateStatus_newToCancelled_success() {
        // Arrange - Order is NEW (status=0)
        testOrder.setStatus(0);
        when(entityManager.find(eq(Order.class), eq(testOrder.getId()), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(testOrder);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        assertDoesNotThrow(() -> {
            orderService.updateStatus(testOrder.getId(), 2);
        });

        // Assert
        verify(orderRepository, times(1)).save(testOrder);
        assertEquals(2, testOrder.getStatus());
    }

    @Test
    @DisplayName("TC-ORD-SER-006: Order Service - Update status with invalid value")
    void test_updateStatus_invalidStatusValue_throwsBusinessRuleException() {
        // Arrange - Order exists
        testOrder.setStatus(0);
        when(entityManager.find(eq(Order.class), eq(testOrder.getId()), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(testOrder);

        // Act & Assert - Try to set invalid status (5)
        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> {
            orderService.updateStatus(testOrder.getId(), 5);
        });

        assertTrue(exception.getMessage().contains("Trạng thái đơn hàng không hợp lệ"));
    }

    @Test
    @DisplayName("TC-ORD-SER-007: Order Service - Cancel order refunds points to account")
    void test_updateStatus_cancelOrder_refundsPoints() {
        // Arrange - Order with points used
        testOrder.setStatus(0);
        testOrder.setPointsUsed(50);
        testAccount.setPoints(100);

        when(entityManager.find(eq(Order.class), eq(testOrder.getId()), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(testOrder);
        when(entityManager.find(eq(Account.class), eq(testAccount.getUsername()), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(testAccount);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        orderService.updateStatus(testOrder.getId(), 2);

        // Assert - Points should be refunded
        assertEquals(150, testAccount.getPoints()); // 100 + 50
        verify(pointHistoryRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("TC-ORD-SER-008: Order Service - FindById returns null when not found")
    void test_findById_orderNotFound_returnsNull() {
        // Arrange
        when(orderRepository.findById(99999L)).thenReturn(Optional.empty());

        // Act
        Order result = orderService.findById(99999L);

        // Assert
        assertNull(result);
    }
}
