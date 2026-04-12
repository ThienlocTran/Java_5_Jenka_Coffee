package com.springboot.jenka_coffee.api;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CSRF Token endpoint - Frontend gọi để lấy CSRF token
 * Token được trả về trong cookie XSRF-TOKEN (auto set bởi CookieCsrfTokenRepository)
 * Frontend cần đọc cookie này và gửi trong header X-XSRF-TOKEN cho mọi POST/PUT/DELETE request
 */
@RestController
@RequestMapping("/api")
public class ApiCsrfController {

    /**
     * GET /api/csrf-token
     * Trả về CSRF token cho frontend
     * Spring Security tự động set cookie XSRF-TOKEN
     */
    @GetMapping("/csrf-token")
    public CsrfToken getCsrfToken(CsrfToken token) {
        // Spring Security tự động inject CsrfToken
        // Cookie XSRF-TOKEN đã được set tự động
        return token;
    }
}
