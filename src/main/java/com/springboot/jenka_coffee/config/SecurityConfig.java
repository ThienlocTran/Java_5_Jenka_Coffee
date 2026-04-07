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

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
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
                    "default-src 'self'; " +
                    "script-src 'self' 'unsafe-inline'; " +
                    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://cdnjs.cloudflare.com; " +
                    "img-src 'self' data: https://res.cloudinary.com https://ui-avatars.com; " +
                    "font-src 'self' https://fonts.gstatic.com https://cdnjs.cloudflare.com; " +
                    "connect-src 'self'"
                ))
            )
            .authorizeHttpRequests(auth -> auth
                // Public — không cần token
                .requestMatchers(HttpMethod.GET,
                    "/api/products/**", "/api/categories/**",
                    "/api/news/**", "/api/banners/**",
                    "/sitemap.xml", "/robots.txt").permitAll()
                .requestMatchers(
                    "/api/auth/**",
                    "/api/contacts",
                    "/api/contact/**",
                    "/api/bookings",
                    "/api/booking/**",
                    "/api/error",
                    "/uploads/**").permitAll()
                // Admin — phải có ROLE_ADMIN
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // User — phải đăng nhập
                .requestMatchers(
                    "/api/cart/**",
                    "/api/orders/**",
                    "/api/profile/**").hasRole("USER")
                // Mọi endpoint không match → deny (secure by default)
                .anyRequest().denyAll()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
