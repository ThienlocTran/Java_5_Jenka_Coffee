package com.springboot.jenka_coffee.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.jenka_coffee.dto.LoginRequest;
import com.springboot.jenka_coffee.dto.SignupRequest;
import com.springboot.jenka_coffee.dto.response.AuthResult;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test Case Document: AUTH MODULE - PUBLIC ENDPOINTS
 * Tests for ApiAuthController PUBLIC endpoints (no authentication required)
 * 
 * Coverage:
 * - Login with valid/invalid credentials
 * - Login with not activated account
 * - Signup with valid/invalid data
 * - Logout
 * - Refresh token
 * 
 * ✅ STRATEGY:
 * - Use addFilters = false to disable ALL filters (including Security)
 * - Test only PUBLIC endpoints that don't require authentication
 * - For AUTHENTICATED endpoints, see ApiAuthControllerAuthenticatedTest
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)  // ✅ Disable filters for public endpoints
@DisplayName("API Auth Controller - Public Endpoints Tests")
class ApiAuthControllerPublicTest {

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

    // ========== HELPER METHODS ==========

    /**
     * Build valid SignupRequest that passes ALL validation rules
     * ✅ Password: Must have uppercase, lowercase, digit, special char (@$!%*?&)
     * ✅ Phone: Must match Vietnamese phone number format
     * ✅ Username: Only alphanumeric and underscore
     */
    private SignupRequest buildValidSignupRequest() {
        SignupRequest request = new SignupRequest();
        request.setUsername("newuser123");
        request.setFullname("New User Test");
        request.setPhone("0901234567");
        request.setEmail("newuser@example.com");
        request.setPassword("Password@123");
        return request;
    }

    // ========== TEST CASES ==========

    @Test
    @DisplayName("Login with valid credentials - Return 200 OK with user data")
    void testLoginSuccess() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setRemember(false);

        Account account = new Account();
        account.setUsername("testuser");
        account.setFullname("Test User");
        account.setEmail("test@example.com");
        account.setAdmin(false);
        account.setActivated(true);

        AuthResult authResult = AuthResult.success(account);

        when(accountService.authenticateWithResult(anyString(), anyString())).thenReturn(authResult);
        when(jwtService.generateAccessToken(anyString(), anyBoolean())).thenReturn("access_token_123");
        when(jwtService.generateRefreshToken(anyString())).thenReturn("refresh_token_123");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Đăng nhập thành công"))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.fullname").value("Test User"))
                .andExpect(jsonPath("$.data.isAdmin").value(false));

        verify(accountService).authenticateWithResult("testuser", "password123");
        verify(jwtService).generateAccessToken("testuser", false);
        verify(jwtService).generateRefreshToken("testuser");
    }

    @Test
    @DisplayName("Login with invalid credentials - Return 401 Unauthorized")
    void testLoginInvalidCredentials() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrongpassword");

        AuthResult authResult = AuthResult.invalidCredentials();

        when(accountService.authenticateWithResult(anyString(), anyString())).thenReturn(authResult);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Sai tên đăng nhập hoặc mật khẩu!"));
    }

    @Test
    @DisplayName("Login with not activated account - Return 401 Unauthorized")
    void testLoginNotActivated() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        Account account = new Account();
        account.setUsername("testuser");
        account.setActivated(false);

        AuthResult authResult = AuthResult.notActivated(account);

        when(accountService.authenticateWithResult(anyString(), anyString())).thenReturn(authResult);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Sai tên đăng nhập hoặc mật khẩu!"));
    }

    @Test
    @DisplayName("Signup with valid data - Return 200 OK")
    void testSignupSuccess() throws Exception {
        // Arrange
        SignupRequest request = buildValidSignupRequest();

        doNothing().when(accountService).register(anyString(), anyString(), anyString(), anyString(), anyString());

        // Act & Assert
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Đăng ký thành công! Vui lòng đăng nhập."));

        verify(accountService).register("newuser123", "New User Test", "0901234567", "newuser@example.com", "Password@123");
    }

    @Test
    @DisplayName("Signup with invalid password format - Return 400 Bad Request")
    void testSignupInvalidPassword() throws Exception {
        // Arrange
        SignupRequest request = buildValidSignupRequest();
        request.setPassword("Password123");  // ❌ Missing special character

        // Act & Assert
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        
        verify(accountService, never()).register(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Signup with invalid phone format - Return 400 Bad Request")
    void testSignupInvalidPhone() throws Exception {
        // Arrange
        SignupRequest request = buildValidSignupRequest();
        request.setPhone("123456");  // ❌ Invalid VN phone format

        // Act & Assert
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        
        verify(accountService, never()).register(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Logout with valid token - Return 200 OK")
    void testLogoutSuccess() throws Exception {
        // Arrange
        when(jwtService.isValid(anyString())).thenReturn(true);
        doNothing().when(jwtBlacklistService).blacklistToken(anyString(), anyLong());
        doNothing().when(cookieService).deleteRememberMeCookie(any());

        // Act & Assert
        mockMvc.perform(post("/api/auth/logout")
                .cookie(new jakarta.servlet.http.Cookie("access_token", "valid_token"))
                .cookie(new jakarta.servlet.http.Cookie("refresh_token", "valid_refresh")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Đã đăng xuất thành công!"));
    }

    @Test
    @DisplayName("Refresh token with valid refresh token - Return 200 OK")
    void testRefreshTokenSuccess() throws Exception {
        // Arrange
        Account account = new Account();
        account.setUsername("testuser");
        account.setFullname("Test User");
        account.setEmail("test@example.com");
        account.setAdmin(false);
        account.setActivated(true);

        when(jwtService.isValid(anyString())).thenReturn(true);
        when(jwtService.isRefreshToken(anyString())).thenReturn(true);
        when(jwtBlacklistService.isBlacklisted(anyString())).thenReturn(false);
        when(jwtService.extractUsername(anyString())).thenReturn("testuser");
        when(accountService.findById(anyString())).thenReturn(account);
        when(jwtService.generateAccessToken(anyString(), anyBoolean())).thenReturn("new_access_token");

        // Act & Assert
        mockMvc.perform(post("/api/auth/refresh")
                .cookie(new jakarta.servlet.http.Cookie("refresh_token", "valid_refresh_token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    @DisplayName("Refresh token with invalid refresh token - Return 401 Unauthorized")
    void testRefreshTokenInvalid() throws Exception {
        // Arrange
        when(jwtService.isValid(anyString())).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/api/auth/refresh")
                .cookie(new jakarta.servlet.http.Cookie("refresh_token", "invalid_token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại."));
    }

    @Test
    @DisplayName("Refresh token with blacklisted token - Return 401 Unauthorized")
    void testRefreshTokenBlacklisted() throws Exception {
        // Arrange
        when(jwtService.isValid(anyString())).thenReturn(true);
        when(jwtService.isRefreshToken(anyString())).thenReturn(true);
        when(jwtBlacklistService.isBlacklisted(anyString())).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/api/auth/refresh")
                .cookie(new jakarta.servlet.http.Cookie("refresh_token", "blacklisted_token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Token đã bị vô hiệu hóa. Vui lòng đăng nhập lại."));
    }
}
