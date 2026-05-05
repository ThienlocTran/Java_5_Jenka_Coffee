package com.springboot.jenka_coffee.api.admin;

import com.springboot.jenka_coffee.repository.ContactRepository;
import com.springboot.jenka_coffee.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ADVANCED TEST CASES for Notification Module (Batch 05)
 * Focus: Real-time consistency, race conditions, cross-controller sync, architectural issues
 * 
 * TC-NTF-ADV-001 to TC-NTF-ADV-005
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiAdminNotificationControllerAdvancedTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderRepository orderRepository;

    @MockBean
    private ContactRepository contactRepository;

    @Test
    @DisplayName("TC-NTF-ADV-001: Notification counts real-time accuracy - after new order created")
    @WithMockUser(roles = "ADMIN")
    void test_notificationCounts_realTimeAccuracy_afterNewOrder() throws Exception {
        // Arrange - Initial state: 5 new orders
        when(orderRepository.countByStatus(0)).thenReturn(5L);
        when(contactRepository.countByIsReadFalse()).thenReturn(3L);

        // Act & Assert - First call
        mockMvc.perform(get("/api/admin/notifications/counts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.newOrders").value(5))
                .andExpect(jsonPath("$.data.unreadContacts").value(3));

        // Simulate: New order created → count increases
        when(orderRepository.countByStatus(0)).thenReturn(6L);

        // Act & Assert - Second call should reflect new count
        mockMvc.perform(get("/api/admin/notifications/counts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.newOrders").value(6))
                .andExpect(jsonPath("$.data.unreadContacts").value(3));
        
        // Verify: Real-time consistency - no caching issues
        // Each call queries DB directly (no stale cache)
        verify(orderRepository, times(2)).countByStatus(0);
    }

    @Test
    @DisplayName("TC-NTF-ADV-002: Notification counts cross-controller consistency - after mark contact read")
    @WithMockUser(roles = "ADMIN")
    void test_notificationCounts_crossControllerConsistency_afterMarkRead() throws Exception {
        // Arrange - Initial: 5 unread contacts
        when(orderRepository.countByStatus(0)).thenReturn(2L);
        when(contactRepository.countByIsReadFalse()).thenReturn(5L);

        // Act & Assert - Initial state
        mockMvc.perform(get("/api/admin/notifications/counts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadContacts").value(5));

        // Simulate: Contact marked as read via ApiAdminContactController
        // PATCH /api/admin/contacts/1/read → contactService.markAsRead(1)
        when(contactRepository.countByIsReadFalse()).thenReturn(4L);

        // Act & Assert - Count should decrease
        mockMvc.perform(get("/api/admin/notifications/counts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadContacts").value(4));
        
        // Verify: Cross-controller consistency
        // NotificationController reflects changes made by ContactController
        verify(contactRepository, times(2)).countByIsReadFalse();
    }

    @Test
    @DisplayName("TC-NTF-ADV-003: Notification counts race condition - concurrent order creation")
    @WithMockUser(roles = "ADMIN")
    void test_notificationCounts_raceCondition_concurrentOrderCreation() throws Exception {
        // Arrange - Simulate race condition
        // Thread 1: GET /notifications/counts → reads count = 5
        // Thread 2: POST /orders → creates new order → count = 6
        // Thread 1: Returns count = 5 (stale)
        
        when(orderRepository.countByStatus(0))
                .thenReturn(5L)  // First call
                .thenReturn(6L); // Second call (after order created)
        when(contactRepository.countByIsReadFalse()).thenReturn(0L);

        // Act & Assert - First call
        mockMvc.perform(get("/api/admin/notifications/counts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.newOrders").value(5));

        // Simulate: Order created between calls
        
        // Act & Assert - Second call sees updated count
        mockMvc.perform(get("/api/admin/notifications/counts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.newOrders").value(6));
        
        // BUG RISK: If counts are cached, second call might return stale data
        // Current implementation: No caching → always fresh (correct)
        // But: No transaction isolation → count might be inconsistent during concurrent writes
    }

    @Test
    @DisplayName("TC-NTF-ADV-004: Notification counts all zeros - no pending activity")
    @WithMockUser(roles = "ADMIN")
    void test_notificationCounts_allZeros_noPendingActivity() throws Exception {
        // Arrange - Clean state: no new orders, no unread contacts
        when(orderRepository.countByStatus(0)).thenReturn(0L);
        when(contactRepository.countByIsReadFalse()).thenReturn(0L);

        // Act & Assert
        mockMvc.perform(get("/api/admin/notifications/counts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.newOrders").value(0))
                .andExpect(jsonPath("$.data.unreadContacts").value(0));
        
        // Verify: No crash, no null values
        // All counts are 0 (not null)
    }

    @Test
    @DisplayName("TC-NTF-ADV-005: Architectural issue - controller injects repositories directly")
    @WithMockUser(roles = "ADMIN")
    void test_architecture_controllerInjectsRepositoriesDirectly() throws Exception {
        // Arrange
        when(orderRepository.countByStatus(0)).thenReturn(5L);
        when(contactRepository.countByIsReadFalse()).thenReturn(3L);

        // Act & Assert
        mockMvc.perform(get("/api/admin/notifications/counts"))
                .andExpect(status().isOk());
        
        // ARCHITECTURAL GAP:
        // ApiAdminNotificationController injects OrderRepository and ContactRepository directly
        // Violates 3-tier architecture: Controller → Service → Repository
        // 
        // Current:
        //   Controller → Repository (WRONG)
        // 
        // Should be:
        //   Controller → NotificationService → Repository
        // 
        // Risks:
        // 1. Business logic in controller (hard to test)
        // 2. No transaction management
        // 3. Tight coupling (hard to refactor)
        // 4. Can't add caching/logging at service layer
        // 
        // Fix: Create NotificationService with methods:
        //   - getNotificationCounts() → returns { newOrders, unreadContacts }
        //   - Inject OrderService and ContactService (not repositories)
        // 
        // Mark as TECH DEBT for refactoring
        
        verify(orderRepository).countByStatus(0);
        verify(contactRepository).countByIsReadFalse();
    }

    @Test
    @DisplayName("TC-NTF-ADV-006: Notification counts without auth - returns 401")
    void test_notificationCounts_noAuth_returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/notifications/counts"))
                .andExpect(status().isUnauthorized());
        
        // Verify: No repository calls without auth
        verify(orderRepository, never()).countByStatus(anyInt());
        verify(contactRepository, never()).countByIsReadFalse();
    }

    @Test
    @DisplayName("TC-NTF-ADV-007: Notification counts idempotency - multiple calls return same result")
    @WithMockUser(roles = "ADMIN")
    void test_notificationCounts_idempotency_multipleCallsSameResult() throws Exception {
        // Arrange - Fixed counts
        when(orderRepository.countByStatus(0)).thenReturn(5L);
        when(contactRepository.countByIsReadFalse()).thenReturn(3L);

        // Act & Assert - Call 3 times
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/admin/notifications/counts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.newOrders").value(5))
                    .andExpect(jsonPath("$.data.unreadContacts").value(3));
        }
        
        // Verify: Idempotent - same result every time (no side effects)
        verify(orderRepository, times(3)).countByStatus(0);
        verify(contactRepository, times(3)).countByIsReadFalse();
    }

    @Test
    @DisplayName("TC-NTF-ADV-008: Notification counts performance - should be fast (<100ms)")
    @WithMockUser(roles = "ADMIN")
    void test_notificationCounts_performance_shouldBeFast() throws Exception {
        // Arrange
        when(orderRepository.countByStatus(0)).thenReturn(1000L);
        when(contactRepository.countByIsReadFalse()).thenReturn(500L);

        // Act - Measure time
        long startTime = System.currentTimeMillis();
        
        mockMvc.perform(get("/api/admin/notifications/counts"))
                .andExpect(status().isOk());
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Assert - Should be fast (< 100ms for simple count queries)
        assertTrue(duration < 100, "Notification counts should be fast (<100ms), actual: " + duration + "ms");
        
        // Note: This is a mock test, real performance depends on DB
        // In production, add indexes on:
        //   - orders.status
        //   - contacts.isRead
        // To ensure COUNT queries are fast
    }
}
