package com.springboot.jenka_coffee.util;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Password Security Utility
 * Handles password hashing and verification using BCrypt
 */
@Component
public class PasswordSecurity {

    private final PasswordEncoder passwordEncoder;

    public PasswordSecurity(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Hash password using BCrypt
     * 
     * @param rawPassword Plain text password
     * @return Hashed password
     */
    public String hashPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * Verify password against hashed password
     * 
     * @param rawPassword    Plain text password to check
     * @param hashedPassword Hashed password from database
     * @return true if password matches, false otherwise
     */
    public boolean verifyPassword(String rawPassword, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, hashedPassword);
    }

    /**
     * Check if password is already hashed (BCrypt format)
     * BCrypt hashes start with $2a$, $2b$, or $2y$
     */
    public boolean isPasswordHashed(String password) {
        if (password == null) {
            return false;
        }
        return password.matches("^\\$2[ayb]\\$.{56}$");
    }
}
