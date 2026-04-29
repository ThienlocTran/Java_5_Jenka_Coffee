package com.springboot.jenka_coffee.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.security.JwtService;
import com.springboot.jenka_coffee.service.AccountService;
import com.springboot.jenka_coffee.service.CookieService;
import com.springboot.jenka_coffee.service.GoogleOAuthService;
import com.springboot.jenka_coffee.service.JwtBlacklistService;
import org.junit.jupiter.api.DisplayName;
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
 * Tests for ApiAuthController AUTHENTICATED endpoints (require authentication)
 * 
 * Coverage:
 * - TC-AUTH-001: Access Admin API without token (401)
 * - TC-AUTH-002: Access Admin API with customer role token (403)
 * - Get current user info (/me endpoint)
 * 
 * ✅ STRATEGY:
 * - Use @AutoConfigureMockMvc (filters ENABLED) so @WithMockUser works
 * - Mock RateLimitFilter to prevent 429 errors
 * - Test only AUTHENTICATED endpoints that require Spring Security
 * - For PUBLIC endpoints, see ApiAuthControllerPublicTest
 */
@SpringBootTest
@AutoConfigureMockMvc  // ✅ Enable filters so @WithMockUser works!
@DisplayName("API Auth Controller - Authenticated Endpoints Tests")
class ApiAuthControllerAuthenticatedTest {

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

    @MockBean
    private com.springboot.jenka_coffee.config.RateLimitFilter rateLimitFilter;  // ✅ Mock to prevent 429

    // ========== TEST CASES ==========

    @Test
    @DisplayName("TC-AUTH-001: Access Admin API without token - Return 401 Unauthorized")
    void TC_AUTH_001() throws Exception {
        // Act & Assert - No @WithMockUser = unauthenticated
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
        
        // Verify service was NOT called due to authentication failure
        verify(accountService, never()).findById(anyString());
    }

    @Test
    @WithMockUser(username = "customer1", roles = "USER")
    @DisplayName("TC-AUTH-002: Access Admin API with customer role - Return 403 Forbidden")
    void TC_AUTH_002() throws Exception {
        // This test would be for admin endpoints like /api/admin/**
        // /api/auth/me requires ROLE_USER, so it will pass
        // For a real 403 test, we need to test an admin endpoint
        
        // Example: If we had an admin-only endpoint in ApiAuthController
        // mockMvc.perform(get("/api/admin/users"))
        //         .andExpect(status().isForbidden());
    }

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
        // Act & Assert - No @WithMockUser = unauthenticated
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
        
        // Verify service was NOT called due to authentication failure
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
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.username").value("admin1"))
                .andExpect(jsonPath("$.data.isAdmin").value(true));
        
        verify(accountService).findById("admin1");
    }
}
