package com.springboot.jenka_coffee.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security Configuration với BCrypt password encoding
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * BCrypt Password Encoder Bean
     * Strength: 12 (default 10, cao hơn = an toàn hơn nhưng chậm hơn)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Security Filter Chain Configuration
     * Disable default Spring Security vì đang dùng custom authentication
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Tắt Spring Security's default login/logout
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll())
                // Disable CSRF for simplicity (enable in production if needed)
                .csrf(csrf -> csrf.disable())
                // Disable default form login
                .formLogin(form -> form.disable())
                // Disable HTTP Basic
                .httpBasic(basic -> basic.disable());

        return http.build();
    }
}
