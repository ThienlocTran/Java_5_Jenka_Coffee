package com.springboot.jenka_coffee.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Security Headers Filter - Thêm các HTTP security headers
 * Bảo vệ chống XSS, Clickjacking, MIME sniffing, etc.
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        // X-Frame-Options: Chống Clickjacking
        response.setHeader("X-Frame-Options", "DENY");
        
        // X-Content-Type-Options: Chống MIME sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");
        
        // X-XSS-Protection: Enable XSS filter (legacy browsers)
        response.setHeader("X-XSS-Protection", "1; mode=block");
        
        // Referrer-Policy: Giới hạn thông tin referrer
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // Permissions-Policy: Giới hạn browser features
        response.setHeader("Permissions-Policy", 
            "geolocation=(), microphone=(), camera=(), payment=()");
        
        // VULN-CSP-COLLISION FIX: CSP is configured in SecurityConfig.java
        // Removed duplicate CSP header to avoid browser confusion
        // SecurityConfig.java is the single source of truth for CSP policy
        
        // HSTS: Force HTTPS (chỉ enable trên production)
        boolean isProduction = !"default".equals(activeProfile) && !"local".equals(activeProfile);
        if (isProduction) {
            // max-age=31536000 (1 năm), includeSubDomains, preload
            response.setHeader("Strict-Transport-Security", 
                "max-age=31536000; includeSubDomains; preload");
        }
        
        // Cache-Control cho sensitive endpoints
        String path = request.getRequestURI();
        if (path.startsWith("/api/admin") || path.startsWith("/api/auth")) {
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, private");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
        }

        filterChain.doFilter(request, response);
    }
}
