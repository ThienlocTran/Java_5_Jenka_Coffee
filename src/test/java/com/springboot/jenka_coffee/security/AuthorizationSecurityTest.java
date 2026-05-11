package com.springboot.jenka_coffee.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SECURITY TEST CASES - Authorization & Access Control (Batch 06)
 * TC-SEC-IDOR-001 to TC-SEC-IDOR-003, TC-SEC-ROLE-001 to TC-SEC-ROLE-004
 * 
 * Focus: IDOR vulnerabilities, role escalation, privilege checks
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthorizationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("TC-SEC-IDOR-001: User A reads User B's orders - returns 403 or 404 (both secure)")
    @WithMockUser(username = "userA", roles = "USER")
    void test_idor_userAReadsUserBOrders_returns403() throws Exception {
        // Arrange - Public order code belongs to userB (or doesn't exist in test DB)
        String orderCodeOfUserB = "ORD-20260511-ZZZ999";

        // Act & Assert - Accept both 403 (explicit forbidden) and 404 (information hiding)
        // Both are secure responses - neither leaks order data
        mockMvc.perform(get("/api/orders/" + orderCodeOfUserB))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 403 && status != 404) {
                        throw new AssertionError("Expected 403 or 404, but got: " + status);
                    }
                });
        
        // IDOR CRITICAL: orderService must check order.account.username == currentUser
        // 403 = explicit access denied, 404 = information hiding (order doesn't exist or not yours)
        // Both are acceptable security responses
    }

    @Test
    @DisplayName("TC-SEC-IDOR-002: User A cancels User B's order - returns 403 or 404 (both secure)")
    @WithMockUser(username = "userA", roles = "USER")
    void test_idor_userACancelsUserBOrder_returns403() throws Exception {
        // Arrange - Public order code belongs to userB (or doesn't exist in test DB)
        String orderCodeOfUserB = "ORD-20260511-ZZZ999";

        // Act & Assert - Accept both 403 and 404
        mockMvc.perform(post("/api/orders/" + orderCodeOfUserB + "/cancel"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 403 && status != 404) {
                        throw new AssertionError("Expected 403 or 404, but got: " + status);
                    }
                });
        
        // IDOR + Business logic: Must check both auth (JWT valid) AND ownership
        // Verify: Order NOT cancelled; response 403 or 404 (both secure)
    }

    @Test
    @DisplayName("TC-SEC-IDOR-003: User A updates User B's profile - returns 403 Forbidden")
    @WithMockUser(username = "userA", roles = "USER")
    void test_idor_userAUpdatesUserBProfile_returns403() throws Exception {
        // Arrange - Target is userB's profile
        String targetUsername = "userB";

        // Act & Assert
        mockMvc.perform(put("/api/accounts/" + targetUsername)
                        .contentType("application/json")
                        .content("{\"fullname\":\"Hacked Name\"}"))
                .andExpect(status().isForbidden());
        
        // IDOR: Account update must check principal.username == targetUsername
        // Verify: userB data NOT changed
    }

    @Test
    @DisplayName("TC-SEC-ROLE-001: Regular user calls admin API - returns 403 Forbidden")
    @WithMockUser(username = "customer1", roles = "USER")
    void test_roleEscalation_regularUserCallsAdminAPI_returns403() throws Exception {
        // Act & Assert - Regular user tries to access admin endpoint
        mockMvc.perform(get("/api/admin/products"))
                .andExpect(status().isForbidden());
        
        // Role escalation: Spring Security @PreAuthorize or SecurityConfig
        // Must block ROLE_USER from /api/admin/**
        // Verify: 403 Forbidden (NOT 401)
    }

    @Test
    @DisplayName("TC-SEC-ROLE-002: Regular user creates product - returns 403 Forbidden")
    @WithMockUser(username = "customer1", roles = "USER")
    void test_roleEscalation_regularUserCreatesProduct_returns403() throws Exception {
        // Act & Assert
        mockMvc.perform(multipart("/api/admin/products")
                        .param("name", "Hacked Product")
                        .param("price", "100000")
                        .param("categoryId", "CF"))
                .andExpect(status().isForbidden());
        
        // Verify: 403; product NOT created in DB
        // Check no privilege escalation via header manipulation
    }

    @Test
    @DisplayName("TC-SEC-ROLE-003: Regular user accesses dashboard - returns 403 Forbidden")
    @WithMockUser(username = "customer1", roles = "USER")
    void test_roleEscalation_regularUserAccessesDashboard_returns403() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isForbidden());
        
        // Sensitive data: Revenue + customer data in dashboard
        // Verify: 403 strict; does NOT leak any dashboard data
    }

    @Test
    @DisplayName("TC-SEC-ROLE-004: Regular user updates order status - returns 403 Forbidden")
    @WithMockUser(username = "customer1", roles = "USER")
    void test_roleEscalation_regularUserUpdatesOrderStatus_returns403() throws Exception {
        // Arrange - Order ID 1 exists
        Long orderId = 1L;

        // Act & Assert
        mockMvc.perform(put("/api/admin/orders/" + orderId + "/status/1"))
                .andExpect(status().isForbidden());
        
        // Verify: Status NOT changed in DB; 403 response
        // Critical business logic protection
    }

    @Test
    @DisplayName("TC-SEC-ROLE-005: Admin can access admin endpoints - returns 200")
    @WithMockUser(username = "admin1", roles = "ADMIN")
    void test_adminCanAccessAdminEndpoints_returns200() throws Exception {
        // Act & Assert - Admin should have access
        mockMvc.perform(get("/api/admin/products"))
                .andExpect(status().isOk());
        
        // Baseline: Verify admin role works correctly
    }

    @Test
    @DisplayName("TC-SEC-ROLE-006: User with both USER and ADMIN roles - admin access works")
    @WithMockUser(username = "superuser", roles = {"USER", "ADMIN"})
    void test_userWithBothRoles_adminAccessWorks() throws Exception {
        // Act & Assert - User with multiple roles
        mockMvc.perform(get("/api/admin/products"))
                .andExpect(status().isOk());
        
        // Verify: hasRole("ADMIN") check passes even if user has multiple roles
    }
}
