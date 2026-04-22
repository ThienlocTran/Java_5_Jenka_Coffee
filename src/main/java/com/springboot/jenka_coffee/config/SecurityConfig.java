package com.springboot.jenka_coffee.config;

import com.springboot.jenka_coffee.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
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
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(f -> f.disable())
            .httpBasic(b -> b.disable())
            .logout(l -> l.disable())
            .headers(headers -> headers
                .frameOptions(fo -> fo.deny())
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
                    "/api/auth/login",
                    "/api/auth/register",
                    "/api/auth/google-login",
                    "/api/auth/refresh",
                    "/api/auth/logout",
                    "/api/auth/send-otp",
                    "/api/auth/verify-otp",
                    "/api/auth/reset-password",
                    "/api/csrf-token", // CSRF token endpoint
                    "/api/contacts",
                    "/api/contact/**",
                    "/api/feedbacks", // Feedback popup - public access
                    "/api/bookings",
                    "/api/booking/**",
                    "/api/visitors/**",
                    "/api/error",
                    "/uploads/**").permitAll()
                // Admin — phải có ROLE_ADMIN
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // User — phải đăng nhập
                .requestMatchers(
                    "/api/auth/update-phone", // Requires authentication
                    "/api/cart/**",
                    "/api/orders/**",
                    "/api/profile/**").hasRole("USER")
                // Mọi endpoint không match → deny (secure by default)
                .anyRequest().denyAll()
            )
            .addFilterBefore(securityHeadersFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
