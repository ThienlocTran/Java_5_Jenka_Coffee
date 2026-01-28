package com.springboot.jenka_coffee.controller.admin;

import com.springboot.jenka_coffee.util.PasswordSecurity;
import org.springframework.web.bind.annotation.*;

/**
 * Utility controller for admin tasks (DEVELOPMENT ONLY - REMOVE IN PRODUCTION)
 */
@RestController
@RequestMapping("/admin/util")
public class AdminUtilController {

    private final PasswordSecurity passwordSecurity;

    public AdminUtilController(PasswordSecurity passwordSecurity) {
        this.passwordSecurity = passwordSecurity;
    }

    /**
     * Generate BCrypt hash for a password
     * DEVELOPMENT ONLY - Remove before production!
     * 
     * Usage: GET /admin/util/hash?password=123
     */
    @GetMapping("/hash")
    public String hashPassword(@RequestParam String password) {
        String hash = passwordSecurity.hashPassword(password);
        return "Password: " + password + "\n" +
                "BCrypt Hash: " + hash + "\n" +
                "Length: " + hash.length() + "\n\n" +
                "SQL Update:\n" +
                "UPDATE Accounts SET password_hash = N'" + hash + "' WHERE username = 'admin';";
    }

    /**
     * Verify password against hash
     * Usage: GET /admin/util/verify?password=123&hash=xxx
     */
    @GetMapping("/verify")
    public String verifyPassword(@RequestParam String password, @RequestParam String hash) {
        boolean match = passwordSecurity.verifyPassword(password, hash);
        return "Password: " + password + "\n" +
                "Hash: " + hash + "\n" +
                "Match: " + match;
    }
}
