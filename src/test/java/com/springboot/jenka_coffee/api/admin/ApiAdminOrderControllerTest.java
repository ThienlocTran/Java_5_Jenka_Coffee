package com.springboot.jenka_coffee.api.admin;

import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.entity.Order;
import com.springboot.jenka_coffee.entity.OrderDetail;
import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.repository.AccountRepository;
import com.springboot.jenka_coffee.repository.OrderRepository;
import com.springboot.jenka_coffee.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ORDER CONTROLLER TEST CASES (Batch 02)
 * TC-ORD-CTRL-001 to TC-ORD-CTRL-017
 * 
 * Focus: Order list pagination, order detail, status updates, cancel operations
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiAdminOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ProductRepository productRepository;

    private Account testAccount;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        // Create test account
        testAccount = new Account();
        testAccount.setUsername("testuser_order");
        testAccount.setPasswordHash("password");
        testAccount.setFullname("Test User");
        testAccount.setEmail("testuser@example.com");
        // FIX: Use unique phone number to avoid duplicate key constraint violation
        // Other test classes use 0123456789, so use different number here
        testAccount.setPhone("0911111111");  // Changed from 0123456789
        testAccount.setAdmin(true);
        testAccount.setActivated(true);
        accountRepository.save(testAccount);

        // Create test order
        testOrder = new Order();
        testOrder.setAccount(testAccount);
        testOrder.setAddress("123 Test St, Ward, District, Province");
        // FIX: Use same phone as testAccount to maintain consistency
        testOrder.setPhone("0911111111");  // Changed from 0123456789
        testOrder.setStatus(0); // NEW
        testOrder.setTotalAmount(new BigDecimal("500000"));
        testOrder.setCreateDate(LocalDateTime.now());
        testOrder.setOrderDetails(new ArrayList<>());
        orderRepository.save(testOrder);
    }

    @Test
    @DisplayName("TC-ORD-CTRL-001: Order - GET list valid request")
    @WithMockUser(roles = "ADMIN")
    void test_getOrderList_validRequest_returns200() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/orders")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))  // Fixed: use $.status not $.success
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.currentPage").value(0))
                .andExpect(jsonPath("$.data.totalPages").exists())
                .andExpect(jsonPath("$.data.totalItems").exists())
                .andExpect(jsonPath("$.data.items[*].account").exists());
    }

    @Test
    @DisplayName("TC-ORD-CTRL-002: Order - GET list page negative (auto-correct)")
    @WithMockUser(roles = "ADMIN")
    void test_getOrderList_pageNegative_autoCorrectTo0() throws Exception {
        // Act & Assert - page=-3 should be auto-corrected to 0
        mockMvc.perform(get("/api/admin/orders")
                        .param("page", "-3")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.currentPage").value(0));
    }

    @Test
    @DisplayName("TC-ORD-CTRL-003: Order - GET list size=0 (boundary - auto-cap to 1)")
    @WithMockUser(roles = "ADMIN")
    void test_getOrderList_sizeZero_autoCapTo1() throws Exception {
        // Act & Assert - size=0 should be auto-capped to 1
        mockMvc.perform(get("/api/admin/orders")
                        .param("page", "0")
                        .param("size", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items").isArray());
        // Note: Cannot directly verify size=1 from response, but code ensures Math.max(size, 1)
    }

    @Test
    @DisplayName("TC-ORD-CTRL-004: Order - GET list size=9999 (auto-cap to 100)")
    @WithMockUser(roles = "ADMIN")
    void test_getOrderList_sizeTooLarge_autoCapTo100() throws Exception {
        // Act & Assert - size=9999 should be auto-capped to 100
        mockMvc.perform(get("/api/admin/orders")
                        .param("page", "0")
                        .param("size", "9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(lessThanOrEqualTo(100)));
    }

    @Test
    @DisplayName("TC-ORD-CTRL-005: Order - GET list page very large with no data")
    @WithMockUser(roles = "ADMIN")
    void test_getOrderList_pageVeryLarge_returnsEmptyList() throws Exception {
        // Act & Assert - page=9999 should return empty items but not crash
        mockMvc.perform(get("/api/admin/orders")
                        .param("page", "9999")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.totalItems").exists());
    }

    @Test
    @DisplayName("TC-ORD-CTRL-006: Order - GET detail valid id")
    @WithMockUser(roles = "ADMIN")
    void test_getOrderDetail_validId_returns200() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/orders/" + testOrder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(testOrder.getId()))
                .andExpect(jsonPath("$.data.address").exists())
                .andExpect(jsonPath("$.data.phone").exists())
                .andExpect(jsonPath("$.data.status").exists())
                .andExpect(jsonPath("$.data.totalAmount").exists())
                .andExpect(jsonPath("$.data.createDate").exists())
                .andExpect(jsonPath("$.data.orderDetails").isArray())
                .andExpect(jsonPath("$.data.account").exists());
    }

    @Test
    @DisplayName("TC-ORD-CTRL-007: Order - GET detail id not found")
    @WithMockUser(roles = "ADMIN")
    void test_getOrderDetail_idNotFound_returns404() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/orders/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("Không tìm thấy")));
    }

    @Test
    @DisplayName("TC-ORD-CTRL-008: Order - UPDATE status to CONFIRMED (status=1)")
    @WithMockUser(roles = "ADMIN")
    void test_updateOrderStatus_toConfirmed_returns200() throws Exception {
        // Arrange - Ensure order is in NEW status
        testOrder.setStatus(0);
        orderRepository.save(testOrder);

        // Act & Assert
        mockMvc.perform(put("/api/admin/orders/" + testOrder.getId() + "/status/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // Verify status changed in DB
        Order updated = orderRepository.findById(testOrder.getId()).orElseThrow();
        assertEquals(1, updated.getStatus(), "Order status should be CONFIRMED (1)");
    }

    @Test
    @DisplayName("TC-ORD-CTRL-009: Order - UPDATE status to CANCELLED (status=2)")
    @WithMockUser(roles = "ADMIN")
    void test_updateOrderStatus_toCancelled_returns200() throws Exception {
        // Arrange - Ensure order is in NEW status
        testOrder.setStatus(0);
        orderRepository.save(testOrder);

        // Act & Assert
        mockMvc.perform(put("/api/admin/orders/" + testOrder.getId() + "/status/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // Verify status changed in DB
        Order updated = orderRepository.findById(testOrder.getId()).orElseThrow();
        assertEquals(2, updated.getStatus(), "Order status should be CANCELLED (2)");
    }

    @Test
    @DisplayName("TC-ORD-CTRL-010: Order - UPDATE status invalid value negative")
    @WithMockUser(roles = "ADMIN")
    void test_updateOrderStatus_negativeValue_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/api/admin/orders/" + testOrder.getId() + "/status/-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("không hợp lệ")));
    }

    @Test
    @DisplayName("TC-ORD-CTRL-011: Order - UPDATE status invalid value out of range")
    @WithMockUser(roles = "ADMIN")
    void test_updateOrderStatus_outOfRange_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/api/admin/orders/" + testOrder.getId() + "/status/5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("không hợp lệ")));
    }

    @Test
    @DisplayName("TC-ORD-CTRL-012: [FIX NEEDED] Order - UPDATE status order not found - should return 404, currently 500")
    @WithMockUser(roles = "ADMIN")
    void test_updateOrderStatus_orderNotFound_shouldReturn404() throws Exception {
        // CSV spec: should be 404 Not Found
        // Current behavior: 500 Internal Server Error (unhandled exception)
        // This test will FAIL until the gap is fixed in OrderService.updateStatus()
        // Fix: catch RuntimeException from EntityManager.find() → throw ResourceNotFoundException → 404
        
        mockMvc.perform(put("/api/admin/orders/99999/status/1"))
                .andExpect(status().isNotFound())  // ✅ Target: 404
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("Không tìm thấy đơn hàng")))
                .andExpect(jsonPath("$.message").value(containsString("99999")));
        
        // If this fails with 500 → gap still exists, fix OrderService
    }

    @Test
    @DisplayName("TC-ORD-CTRL-013: Order - UPDATE status on already CANCELLED order")
    @WithMockUser(roles = "ADMIN")
    void test_updateOrderStatus_alreadyCancelled_returns400() throws Exception {
        // Arrange - Set order to CANCELLED
        testOrder.setStatus(2);
        orderRepository.save(testOrder);

        // Act & Assert - Try to change to CONFIRMED
        mockMvc.perform(put("/api/admin/orders/" + testOrder.getId() + "/status/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("TC-ORD-CTRL-014: Order - CANCEL order valid id via POST")
    @WithMockUser(roles = "ADMIN")
    void test_cancelOrder_validId_returns200() throws Exception {
        // Arrange - Ensure order is in NEW status
        testOrder.setStatus(0);
        orderRepository.save(testOrder);

        // Act & Assert
        mockMvc.perform(post("/api/admin/orders/" + testOrder.getId() + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // Verify status changed to CANCELLED (2)
        Order updated = orderRepository.findById(testOrder.getId()).orElseThrow();
        assertEquals(2, updated.getStatus(), "Order status should be CANCELLED (2)");
    }

    @Test
    @DisplayName("TC-ORD-CTRL-015: Order - CANCEL order already cancelled")
    @WithMockUser(roles = "ADMIN")
    void test_cancelOrder_alreadyCancelled_returns400() throws Exception {
        // Arrange - Set order to CANCELLED
        testOrder.setStatus(2);
        orderRepository.save(testOrder);

        // Act & Assert
        mockMvc.perform(post("/api/admin/orders/" + testOrder.getId() + "/cancel"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    @Test
    @DisplayName("TC-ORD-CTRL-016: [FIX NEEDED] Order - CANCEL order id not found - should return 400, currently 500")
    @WithMockUser(roles = "ADMIN")
    void test_cancelOrder_idNotFound_shouldReturn400() throws Exception {
        // CSV spec: should be 400 Bad Request
        // Current behavior: 500 Internal Server Error (unhandled exception)
        // This test will FAIL until the gap is fixed in OrderService.cancelOrder()
        
        mockMvc.perform(post("/api/admin/orders/99999/cancel"))
                .andExpect(status().isBadRequest())  // ✅ CSV spec: 400
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("Không tìm thấy")));
        
        // If this fails with 500 → gap still exists, fix OrderService
    }

    @Test
    @DisplayName("TC-ORD-CTRL-017: Order - DELETE (legacy cancel) valid id")
    @WithMockUser(roles = "ADMIN")
    void test_deleteOrder_validId_returns200() throws Exception {
        // Arrange - Ensure order is in NEW status
        testOrder.setStatus(0);
        orderRepository.save(testOrder);

        // Act & Assert - DELETE delegates to cancelOrder
        mockMvc.perform(delete("/api/admin/orders/" + testOrder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // Verify status changed to CANCELLED (2)
        Order updated = orderRepository.findById(testOrder.getId()).orElseThrow();
        assertEquals(2, updated.getStatus(), "Order status should be CANCELLED (2)");
    }
}
