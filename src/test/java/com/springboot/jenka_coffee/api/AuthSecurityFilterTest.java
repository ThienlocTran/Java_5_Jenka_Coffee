package com.springboot.jenka_coffee.api;

import com.springboot.jenka_coffee.config.RateLimitFilter;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security Filter Integration Test
 * Test JwtAuthFilter với token THẬT (không mock filter, không bypass security)
 * 
 * ⚠️ KHÔNG dùng @WithMockUser ở đây – phải test filter thật
 * ⚠️ KHÔNG dùng addFilters=false
 */
@SpringBootTest
@AutoConfigureMockMvc  // Filters ENABLED
@DisplayName("JWT Auth Filter - Security Tests")
class AuthSecurityFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RateLimitFilter rateLimitFilter; // Mock rate limit để không bị 429

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    // ─── A. TOKEN EXPIRED ────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-SEC-AUTH-001: Expired access token → 401 (KHÔNG phải 500)")
    void expiredToken_shouldReturn401() throws Exception {
        // Tạo token với expiry = 1 giây trước
        String expiredToken = Jwts.builder()
                .subject("anyuser")
                .claim("type", "access")
                .claim("admin", false)
                .issuedAt(new Date(System.currentTimeMillis() - 3600_000L))
                .expiration(new Date(System.currentTimeMillis() - 1000L)) // expired
                .signWith(getSecretKey())
                .compact();

        mockMvc.perform(get("/api/auth/me")
                .cookie(new Cookie("access_token", expiredToken)))
                .andExpect(status().isUnauthorized()); // 401

        // Nếu trả 500 → JwtAuthFilter không handle ExpiredJwtException → BUG
    }

    // ─── B. TOKEN MALFORMED ──────────────────────────────────────────────────

    @ParameterizedTest(name = "Malformed token [{0}] → 401 not 500")
    @ValueSource(strings = {
        "not.a.valid.jwt",
        "abc",
        "eyJhbGciOiJIUzI1NiJ9.badbody.badsig",
    })
    @DisplayName("TC-SEC-AUTH-002: Malformed token → 401 (KHÔNG crash 500)")
    void malformedToken_shouldReturn401(String badToken) throws Exception {
        mockMvc.perform(get("/api/auth/me")
                .cookie(new Cookie("access_token", badToken)))
                .andExpect(status().isUnauthorized()); // 401, KHÔNG 500

        // isValid() bắt JwtException → return false → filter không set context → Spring Security 401
    }

    // ─── C. AUTHORIZATION HEADER MANIPULATION ───────────────────────────────

    @ParameterizedTest(name = "Header [{0}] → 401")
    @ValueSource(strings = {
        "Bearer ",           // empty sau Bearer
        "Bearer null",       // literal null string
        "null",              // no Bearer prefix
        "Basic dXNlcjpwYXNz" // wrong scheme
    })
    @DisplayName("TC-SEC-AUTH-007/008/009: Header manipulation → 401 KHÔNG crash")
    void headerManipulation_shouldReturn401(String authHeader) throws Exception {
        // extractToken() chỉ xử lý cookie và "Bearer " prefix
        // Các header sai → extractToken() return null → filter skip → 401
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", authHeader))
                .andExpect(status().isUnauthorized()); // 401
    }

    // ─── D. NO TOKEN AT ALL ──────────────────────────────────────────────────

    @Test
    @DisplayName("TC-AUTH-001 (filter): No token → 401")
    void noToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/auth/me")) // không có cookie, không có header
                .andExpect(status().isUnauthorized()); // 401
    }

    // ─── E. REFRESH TOKEN USED AS ACCESS TOKEN ───────────────────────────────

    @Test
    @DisplayName("Refresh token used as access_token cookie → 401 (type check)")
    void refreshTokenUsedAsAccessToken_shouldReturn401() throws Exception {
        // Tạo refresh token (type=refresh) nhưng gửi như access_token cookie
        String refreshTokenAsAccess = Jwts.builder()
                .subject("anyuser")
                .claim("type", "refresh") // ← type = refresh, không phải access
                .claim("admin", false)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400_000L))
                .signWith(getSecretKey())
                .compact();

        mockMvc.perform(get("/api/auth/me")
                .cookie(new Cookie("access_token", refreshTokenAsAccess)))
                .andExpect(status().isUnauthorized()); // 401

        // JwtAuthFilter: isAccessToken(token) = false → skip → 401
    }
}
