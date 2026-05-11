package com.springboot.jenka_coffee.config;

import com.springboot.jenka_coffee.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final SecurityHeadersFilter securityHeadersFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, SecurityHeadersFilter securityHeadersFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.securityHeadersFilter = securityHeadersFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // BUG-49 WARNING: CSRF Protection Disabled - Subdomain Attack Risk
            // 
            // PROBLEM: SameSite=Lax cookies allow subdomain requests
            // - Cookie SameSite=Lax prevents CSRF from external sites (hacker.com)
            // - BUT allows requests from same-site subdomains (blog.jenkacoffee.com)
            // - If customer creates subdomain with XSS vulnerability, attacker can:
            //   1. Inject malicious script on blog.jenkacoffee.com
            //   2. Script makes requests to api.jenkacoffee.com
            //   3. Browser sends cookies because same eTLD+1 domain
            //   4. Attacker can perform actions as authenticated user
            //
            // CURRENT MITIGATION:
            // 1. JWT in HttpOnly cookies with SameSite=Lax (blocks external CSRF)
            // 2. No state-changing GET requests (all mutations use POST/PUT/DELETE)
            // 3. Explicit CORS configuration
            // 4. All sensitive operations require authentication
            //
            // PRODUCTION RECOMMENDATIONS:
            // 1. Enable CSRF protection if using subdomains:
            //    .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
            // 2. Use SameSite=Strict for maximum security (breaks some OAuth flows)
            // 3. Implement custom CSRF token in request headers
            // 4. Avoid hosting user-generated content on subdomains
            // 5. Use separate domain for API (api.example.com) vs app (app.example.com)
            //
            // RISK LEVEL: Medium (requires subdomain + XSS vulnerability)
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            // FIX: Add AuthenticationEntryPoint to return 401 for unauthenticated requests
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED); // 401
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"status\":\"ERROR\",\"message\":\"Vui lòng đăng nhập để tiếp tục!\"}");
                })
            )
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                .contentTypeOptions(cto -> {})
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000))
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    // VULN-L03 FIX: Bỏ 'unsafe-inline' cho script-src
                    // Vue SPA build output dùng external JS files — không cần inline scripts
                    // style-src vẫn cần 'unsafe-inline' vì Vue inject CSS động
                    "default-src 'self'; " +
                    "script-src 'self'; " +
                    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://cdnjs.cloudflare.com; " +
                    "img-src 'self' data: https://res.cloudinary.com https://ui-avatars.com https://images.unsplash.com; " +
                    "font-src 'self' https://fonts.gstatic.com https://cdnjs.cloudflare.com; " +
                    "connect-src 'self' http://localhost:8080 https://java5jenkacoffee-production.up.railway.app; " +
                    "frame-src https://www.google.com; " +
                    "object-src 'none'"
                ))
            )
            .authorizeHttpRequests(auth -> auth
                // Public — không cần token
                .requestMatchers(HttpMethod.GET,
                    "/api/products/**", "/api/categories/**",
                    "/api/news/**", "/api/banners/**",
                    "/sitemap.xml", "/robots.txt").permitAll()
                .requestMatchers(
                    // VULN #15 FIX: Sync with actual controller endpoints
                    // Auth endpoints - must match @PostMapping/@GetMapping paths in ApiAuthController
                    "/api/auth/login",
                    "/api/auth/signup",           // FIX: was /register, actual endpoint is /signup
                    "/api/auth/activate",         // FIX: was missing
                    "/api/auth/forgot-password",  // FIX: was missing
                    "/api/auth/reset-password",   // FIX: was missing
                    "/api/auth/google-login",
                    "/api/auth/refresh",
                    "/api/auth/logout",
                    "/api/auth/check-remember",   // Check remember me cookie
                    "/api/auth/send-otp",
                    "/api/auth/resend-otp",       // FIX: was missing
                    "/api/auth/verify-otp",
                    // VULN #24 FIX: Removed /api/csrf-token endpoint
                    // CSRF protection is disabled (using JWT + SameSite cookies instead)
                    // Having unused CSRF endpoint creates false sense of security
                    "/api/contacts",
                    "/api/contact/**",
                    "/api/feedbacks",             // Feedback popup - public access
                    "/api/bookings",
                    "/api/booking/**",
                    "/api/visitors/**",
                    "/api/cart/**",               // Allow anonymous cart (session-based)
                    "/api/error",
                    "/uploads/**").permitAll()
                // Admin — phải có ROLE_ADMIN
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // User — phải đăng nhập
                .requestMatchers(
                    "/api/auth/me",
                    "/api/auth/update-phone").authenticated()  // Allow any authenticated user (including new Google users without role yet)
                .requestMatchers(
                    "/api/orders/**",
                    "/api/profile/**").hasAnyRole("USER", "ADMIN")
                // Mọi endpoint không match → deny (secure by default)
                .anyRequest().denyAll()
            )
            .addFilterBefore(securityHeadersFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
