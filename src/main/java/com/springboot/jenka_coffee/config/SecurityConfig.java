package com.springboot.jenka_coffee.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.NullSecurityContextRepository;

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
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .httpBasic(basic -> basic.disable())
                // IF_REQUIRED: Spring Security không tự tạo session nhưng dùng session có sẵn
                // Tránh StackOverflow bug của 6.2.x mà vẫn cho phép HttpSession hoạt động
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .securityContext(ctx -> ctx
                        .securityContextRepository(new NullSecurityContextRepository()));

        return http.build();
    }
}
