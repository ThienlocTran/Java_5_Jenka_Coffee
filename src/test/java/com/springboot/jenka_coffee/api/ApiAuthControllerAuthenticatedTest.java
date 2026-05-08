package com.springboot.jenka_coffee.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.security.JwtService;
import com.springboot.jenka_coffee.service.AccountService;
import com.springboot.jenka_coffee.service.CookieService;
import com.springboot.jenka_coffee.service.GoogleOAuthService;
import com.springboot.jenka_coffee.service.JwtBlacklistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test Case Document: AUTH MODULE - AUTHENTICATED ENDPOINTS
 *
 * ROOT CAUSE of "No value at JSON path $.status":
 *   @MockBean RateLimitFilter / JwtAuthFilter KHÔNG bypass filter chain thật.
 *   Spring Boot đăng ký @Component filters vào servlet container độc lập.
 *   JwtAuthFilter chạy trước @WithMockUser → thấy không có cookie/header hợp lệ
 *   → KHÔNG set SecurityContext → principal = null trong controller
 *   → controller trả 401 via AuthenticationEntryPoint (plain JSON, không có $.status=SUCCESS)
 *
 * FIX STRATEGY — dùng @Nested class với 2 chiến lược:
 *   1. SecurityLayerTests: addFilters=true (default) — test Spring Security reject behavior
 *   2. ControllerLogicTests: addFilters=false — bypass filter chain, test controller logic thuần
 */
@SpringBootTest
@DisplayName("API Auth Controller - Authenticated Endpoints Tests")
class ApiAuthControllerAuthenticatedTest {

    // =============================================================================
    // INNER CLASS 1: SECURITY LAYER TESTS — Filters ENABLED
    // Test hành vi của Spring Security: reject unauthenticated, reject wrong role
    // =============================================================================
    @Nested
    @AutoConfigureMockMvc  // Filters ON — test Spring Security behavior
    @DisplayName("Security Layer Tests (Filters Enabled)")
    class SecurityLayerTests {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private AccountService accountService;

        @MockBean
        private JwtService jwtService;

        @MockBean
        private GoogleOAuthService googleOAuthService;

        @MockBean
        private JwtBlacklistService jwtBlacklistService;

        @MockBean
        private CookieService cookieService;

        @Test
        @DisplayName("TC-AUTH-001: Access /api/auth/me without token - Return 401 Unauthorized")
        void TC_AUTH_001_noToken_returns401() throws Exception {
            // No @WithMockUser = không có cookie/header token → AuthenticationEntryPoint → 401
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isUnauthorized());

            // Service KHÔNG được gọi vì bị block ở filter layer
            verify(accountService, never()).findById(anyString());
        }

        @Test
        @WithMockUser(username = "customer1", roles = "USER")
        @DisplayName("TC-AUTH-002: ROLE_USER accesses /api/admin/** - Return 403 Forbidden")
        void TC_AUTH_002_roleUser_callsAdminEndpoint_returns403() throws Exception {
            // ROLE_USER gọi admin endpoint → SecurityConfig: hasRole("ADMIN") → 403
            mockMvc.perform(get("/api/admin/products"))
                    .andExpect(status().isForbidden());

            verify(accountService, never()).findById(anyString());
        }
    }

    // =============================================================================
    // INNER CLASS 2: CONTROLLER LOGIC TESTS — Filters DISABLED
    // Test response format, JSON path, business logic của controller
    // addFilters=false → @WithMockUser inject SecurityContext trực tiếp, không bị JwtAuthFilter overwrite
    // =============================================================================
    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc(addFilters = false)  // KEY FIX: bypass JwtAuthFilter + RateLimitFilter
    @DisplayName("Controller Logic Tests (Filters Disabled)")
    class ControllerLogicTests {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private AccountService accountService;

        @MockBean
        private JwtService jwtService;

        @MockBean
        private GoogleOAuthService googleOAuthService;

        @MockBean
        private JwtBlacklistService jwtBlacklistService;

        @MockBean
        private CookieService cookieService;

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        @DisplayName("Get current user info with valid authentication - Return 200 OK")
        void testGetMeSuccess() throws Exception {
            // Arrange
            Account account = new Account();
            account.setUsername("testuser");
            account.setFullname("Test User");
            account.setEmail("test@example.com");
            account.setAdmin(false);
            account.setPoints(0);

            when(accountService.findById("testuser")).thenReturn(account);

            // Act & Assert
            // addFilters=false: @WithMockUser principal đến controller trực tiếp
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.username").value("testuser"))
                    .andExpect(jsonPath("$.data.fullname").value("Test User"));

            verify(accountService).findById("testuser");
        }

        @Test
        @DisplayName("Get current user info without authentication - Return 401 Unauthorized")
        void testGetMeUnauthorized() throws Exception {
            // No @WithMockUser → principal = null → controller returns 401
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isUnauthorized());

            verify(accountService, never()).findById(anyString());
        }

        @Test
        @WithMockUser(username = "admin1", roles = "ADMIN")
        @DisplayName("Get current user info with admin role - Return 200 OK")
        void testGetMeAsAdmin() throws Exception {
            // Arrange
            Account account = new Account();
            account.setUsername("admin1");
            account.setFullname("Admin User");
            account.setEmail("admin@example.com");
            account.setAdmin(true);
            account.setPoints(0);

            when(accountService.findById("admin1")).thenReturn(account);

            // Act & Assert
            // NOTE: addFilters=false bypasses SecurityConfig's authenticated() check.
            // This is intentional: we are testing CONTROLLER logic, not security rules.
            // Security behavior (role-based access) is tested in SecurityLayerTests above.
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.username").value("admin1"))
                    .andExpect(jsonPath("$.data.isAdmin").value(true));

            verify(accountService).findById("admin1");
        }
    }
}
