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
     * Completely disable Spring Security default behavior
     * We use custom session-based authentication in AuthController
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Permit all requests - custom authentication handled in AuthController
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll())
                // Disable CSRF
                .csrf(csrf -> csrf.disable())
                // Disable default form login (prevents auto-generated password)
                .formLogin(form -> form.disable())
                // Disable logout
                .logout(logout -> logout.disable())
                // Disable HTTP Basic
                .httpBasic(basic -> basic.disable());

        return http.build();
    }
}
