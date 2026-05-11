package com.springboot.jenka_coffee.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.jenka_coffee.dto.LoginRequest;
import com.springboot.jenka_coffee.dto.response.AuthResult;
import com.springboot.jenka_coffee.service.AccountService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rate Limit Test – RateLimitFilter THẬT (KHÔNG mock)
 * 
 * ⚠️ KHÔNG mock RateLimitFilter ở đây – phải test filter thật
 * ⚠️ Rate limit: 10 req/phút per IP → lần 11 phải là 429
 * 
 * Note: RateLimitFilter dùng IP-based bucket. Trong test, MockMvc dùng IP 127.0.0.1.
 * @DirtiesContext đảm bảo test này chạy với Spring context sạch (bucket reset).
 */
@SpringBootTest
@AutoConfigureMockMvc  // Filters ENABLED – cả RateLimitFilter và JwtAuthFilter
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@DisplayName("TC-SEC-AUTH-006: Brute Force Rate Limit Test")
class AuthRateLimitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    @MockBean
    private com.springboot.jenka_coffee.security.JwtService jwtService;

    @MockBean
    private com.springboot.jenka_coffee.service.JwtBlacklistService jwtBlacklistService;

    @MockBean
    private com.springboot.jenka_coffee.service.GoogleOAuthService googleOAuthService;

    @MockBean
    private com.springboot.jenka_coffee.service.CookieService cookieService;
    
    @MockBean
    private com.springboot.jenka_coffee.service.EmailService emailService;
    
    @MockBean
    private com.springboot.jenka_coffee.service.OTPService otpService;

    @Test
    @DisplayName("TC-SEC-AUTH-006: 10 failed logins → lần 11 trả 429 Too Many Requests")
    void bruteForceLogin_afterTenAttempts_shouldReturn429() throws Exception {
        // Mock: login luôn fail
        when(accountService.authenticateWithResult(anyString(), anyString()))
                .thenReturn(AuthResult.invalidCredentials());

        LoginRequest request = new LoginRequest();
        request.setUsername("victim");
        request.setPassword("wrongpassword");

        String body = objectMapper.writeValueAsString(request);

        // Gửi đúng 10 request (limit = 10/phút theo RateLimitFilter)
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body));
            // Các request này có thể là 401 (credentials sai)
        }

        // Request thứ 11 PHẢI bị block bởi RateLimitFilter
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isTooManyRequests()); // 429

        // Nếu vẫn là 401 → RateLimitFilter không hoạt động → BUG
    }
}
