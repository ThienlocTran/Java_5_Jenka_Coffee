package com.springboot.jenka_coffee.api.admin;

import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.entity.Order;
import com.springboot.jenka_coffee.repository.AccountRepository;
import com.springboot.jenka_coffee.repository.OrderRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Admin Order Controller Tests (Real Flow)")
class ApiAdminOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private AccountRepository accountRepository;

    private Order newOrder;
    private Order cancelledOrder;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        accountRepository.deleteAll();

        Account account = new Account();
        account.setUsername("testuser");
        account.setFullname("Test User");
        account.setEmail("test@test.com");
        account.setPhone("0123456789");
        account.setPasswordHash("hash");
        accountRepository.save(account);

        newOrder = new Order();
        newOrder.setStatus(0); // NEW
        newOrder.setTotalAmount(new BigDecimal("150000"));
        newOrder.setCreateDate(LocalDateTime.now());
        newOrder.setAccount(account);
        newOrder = orderRepository.save(newOrder);

        cancelledOrder = new Order();
        cancelledOrder.setStatus(2); // CANCELLED
        cancelledOrder.setTotalAmount(new BigDecimal("200000"));
        cancelledOrder.setCreateDate(LocalDateTime.now());
        cancelledOrder.setAccount(account);
        cancelledOrder = orderRepository.save(cancelledOrder);
    }

    // --- 1. GET LIST TESTS ---

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ORD-CTRL-001: GET list valid request")
    void test_getOrders_validRequest_returns200() throws Exception {
        mockMvc.perform(get("/api/admin/orders")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.totalItems").value(2));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ORD-CTRL-002: GET list page negative (auto-correct)")
    void test_getOrders_negativePage_autoCorrects() throws Exception {
        mockMvc.perform(get("/api/admin/orders")
                .param("page", "-3")
                .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ORD-CTRL-003: GET list size=0 (boundary - auto-cap to 1)")
    void test_getOrders_sizeZero_autoCaps() throws Exception {
        mockMvc.perform(get("/api/admin/orders")
                .param("page", "0")
                .param("size", "0"))
                .andExpect(status().isOk()); // If it 500s here, it's a JPA error
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ORD-CTRL-004: GET list size=9999 (auto-cap to 100)")
    void test_getOrders_largeSize_autoCaps() throws Exception {
        mockMvc.perform(get("/api/admin/orders")
                .param("page", "0")
                .param("size", "9999"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ORD-CTRL-005: GET list page very large with no data")
    void test_getOrders_veryLargePage_returnsEmptyItems() throws Exception {
        mockMvc.perform(get("/api/admin/orders")
                .param("page", "9999")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    // --- 2. GET DETAIL TESTS ---

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ORD-CTRL-006: GET detail valid id")
    void test_getOrderDetail_validId_returns200() throws Exception {
        mockMvc.perform(get("/api/admin/orders/" + newOrder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(newOrder.getId().intValue()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ORD-CTRL-007: GET detail id not found")
    void test_getOrderDetail_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/admin/orders/99999"))
                .andExpect(status().isNotFound()); // Might be 500 in current codebase
    }

    // --- 3. UPDATE STATUS TESTS ---

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ORD-CTRL-008: UPDATE status to CONFIRMED (status=1)")
    void test_updateOrderStatus_toConfirmed_returns200() throws Exception {
        mockMvc.perform(put("/api/admin/orders/" + newOrder.getId() + "/status/1"))
                .andExpect(status().isOk());

        Order updated = orderRepository.findById(newOrder.getId()).orElseThrow();
        assertEquals(1, updated.getStatus());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ORD-CTRL-009: UPDATE status to CANCELLED (status=2)")
    void test_updateOrderStatus_toCancelled_returns200() throws Exception {
        mockMvc.perform(put("/api/admin/orders/" + newOrder.getId() + "/status/2"))
                .andExpect(status().isOk());

        Order updated = orderRepository.findById(newOrder.getId()).orElseThrow();
        assertEquals(2, updated.getStatus());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ORD-CTRL-010: UPDATE status invalid negative")
    void test_updateOrderStatus_invalidNegativeStatus_returns400() throws Exception {
        mockMvc.perform(put("/api/admin/orders/" + newOrder.getId() + "/status/-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ORD-CTRL-011: UPDATE status invalid out of range")
    void test_updateOrderStatus_invalidOutOfRangeStatus_returns400() throws Exception {
        mockMvc.perform(put("/api/admin/orders/" + newOrder.getId() + "/status/5"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ORD-CTRL-012: UPDATE status order id not found")
    void test_updateOrderStatus_notFound_returns404() throws Exception {
        // Correcting the expectation from 500 to 404 to DETECT the bug instead of accepting it
        mockMvc.perform(put("/api/admin/orders/99999/status/1"))
                .andExpect(status().isNotFound()); // Expect 404, if it throws 500, test will fail!
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ORD-CTRL-013: UPDATE status on already CANCELLED order")
    void test_updateOrderStatus_alreadyCancelled_returns400() throws Exception {
        mockMvc.perform(put("/api/admin/orders/" + cancelledOrder.getId() + "/status/1"))
                .andExpect(status().isBadRequest()); // Business Rule Exception
    }

    // --- 4. CANCEL & DELETE TESTS ---

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ORD-CTRL-014: CANCEL order valid id via POST")
    void test_cancelOrder_validId_returns200() throws Exception {
        mockMvc.perform(post("/api/admin/orders/" + newOrder.getId() + "/cancel"))
                .andExpect(status().isOk());

        Order updated = orderRepository.findById(newOrder.getId()).orElseThrow();
        assertEquals(2, updated.getStatus());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ORD-CTRL-015: CANCEL order already cancelled")
    void test_cancelOrder_alreadyCancelled_returns400() throws Exception {
        mockMvc.perform(post("/api/admin/orders/" + cancelledOrder.getId() + "/cancel"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ORD-CTRL-016: CANCEL order id not found (POST)")
    void test_cancelOrder_notFound_returns400() throws Exception {
        mockMvc.perform(post("/api/admin/orders/99999/cancel"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ORD-CTRL-017: DELETE (legacy cancel) valid id")
    void test_deleteOrder_validId_returns200() throws Exception {
        mockMvc.perform(delete("/api/admin/orders/" + newOrder.getId()))
                .andExpect(status().isOk());

        Order updated = orderRepository.findById(newOrder.getId()).orElseThrow();
        assertEquals(2, updated.getStatus()); // Legacy delete maps to cancel
    }
}
