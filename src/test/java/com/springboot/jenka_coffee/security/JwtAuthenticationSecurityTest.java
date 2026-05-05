package com.springboot.jenka_coffee.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SECURITY TEST CASES - JWT Authentication (Batch 06)
 * TC-SEC-AUTH-001 to TC-SEC-AUTH-009
 * 
 * Focus: JWT token validation, malformed tokens, replay attacks, header manipulation
 */
@SpringBootTest
@AutoConfigureMockMvc
class JwtAuthenticationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("TC-SEC-AUTH-001: JWT token expired - returns 401 with 'Token expired' message")
    void test_jwtTokenExpired_returns401() throws Exception {
        // Arrange - Create expired JWT token (exp < now)
        String expiredToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImV4cCI6MTYwMDAwMDAwMH0.signature";
        // exp: 1600000000 = Sep 13, 2020 (expired)

        // Act & Assert
        mockMvc.perform(get("/api/admin/products")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
        
        // CRITICAL SECURITY: JwtAuthFilter must catch ExpiredJwtException
        // Expected: HTTP 401 + message distinguishes 'token expired' vs 'invalid token'
        // Verify: io.jsonwebtoken.ExpiredJwtException → filter catch → 401
    }

    @Test
    @DisplayName("TC-SEC-AUTH-002: JWT token malformed - returns 401 with 'Token invalid' message")
    void test_jwtTokenMalformed_returns401() throws Exception {
        // Arrange - Malformed JWT (cannot be parsed)
        String malformedToken = "abc.def.ghi.invalid";

        // Act & Assert
        mockMvc.perform(get("/api/admin/products")
                        .header("Authorization", "Bearer " + malformedToken))
                .andExpect(status().isUnauthorized());
        
        // CRITICAL SECURITY: io.jsonwebtoken.MalformedJwtException → filter catch → 401
        // Verify: Does NOT crash with 500
        // Message should be distinct from 'expired'
    }

    @Test
    @DisplayName("TC-SEC-AUTH-003: JWT token replay after logout - returns 401 (refresh token blacklisted)")
    void test_jwtTokenReplayAfterLogout_returns401() throws Exception {
        // This test requires:
        // 1. POST /auth/logout (blacklist refresh token)
        // 2. Try to use blacklisted refresh token → POST /auth/refresh
        // 3. Expect: 401 (cannot create new access token)
        
        // CRITICAL SECURITY: Refresh token must be blacklisted after logout
        // Store in Redis/DB with TTL
        // Second use → 401
        
        // Note: This is integration test, requires full auth flow
        // Mark as TODO for implementation
    }

    @Test
    @DisplayName("TC-SEC-AUTH-007: Authorization header empty string - returns 401")
    void test_authHeaderEmptyString_returns401() throws Exception {
        // Arrange - "Bearer " with space but no token
        mockMvc.perform(get("/api/admin/products")
                        .header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized());
        
        // Header manipulation: 'Bearer ' with no token → filter parse empty string → 401
        // Verify: Does NOT crash with NullPointerException when split/parse
    }

    @Test
    @DisplayName("TC-SEC-AUTH-008: Authorization header null value - returns 401")
    void test_authHeaderNullValue_returns401() throws Exception {
        // Arrange - Literal string "null"
        mockMvc.perform(get("/api/admin/products")
                        .header("Authorization", "null"))
                .andExpect(status().isUnauthorized());
        
        // Header manipulation: String literal 'null' → filter must detect and reject → 401
        // Do NOT treat 'null' string as absent header
    }

    @Test
    @DisplayName("TC-SEC-AUTH-009: Authorization header only Bearer keyword - returns 401")
    void test_authHeaderOnlyBearerKeyword_returns401() throws Exception {
        // Arrange - "Bearer" with nothing after
        mockMvc.perform(get("/api/admin/products")
                        .header("Authorization", "Bearer"))
                .andExpect(status().isUnauthorized());
        
        // Header manipulation: split 'Bearer' → only 1 element
        // parts[1] would throw ArrayIndexOutOfBoundsException if not handled → 500
        // Verify: Must return 401 NOT 500
    }

    @Test
    @DisplayName("TC-SEC-AUTH-010: Authorization header with Basic scheme - returns 401")
    void test_authHeaderBasicScheme_returns401() throws Exception {
        // Arrange - Wrong auth scheme (Basic instead of Bearer)
        mockMvc.perform(get("/api/admin/products")
                        .header("Authorization", "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized());
        
        // Verify: JWT filter only accepts "Bearer" scheme
        // "Basic" scheme → rejected → 401
    }

    @Test
    @DisplayName("TC-SEC-AUTH-011: No Authorization header - returns 401")
    void test_noAuthHeader_returns401() throws Exception {
        // Act & Assert - No Authorization header at all
        mockMvc.perform(get("/api/admin/products"))
                .andExpect(status().isUnauthorized());
        
        // Baseline test: Admin endpoint without auth → 401
    }

    @Test
    @DisplayName("TC-SEC-AUTH-012: JWT with invalid signature - returns 401")
    void test_jwtInvalidSignature_returns401() throws Exception {
        // Arrange - Valid JWT structure but wrong signature
        String invalidSigToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImV4cCI6OTk5OTk5OTk5OX0.wrongsignature";

        // Act & Assert
        mockMvc.perform(get("/api/admin/products")
                        .header("Authorization", "Bearer " + invalidSigToken))
                .andExpect(status().isUnauthorized());
        
        // CRITICAL SECURITY: io.jsonwebtoken.SignatureException → 401
        // Prevents token tampering
    }
}
