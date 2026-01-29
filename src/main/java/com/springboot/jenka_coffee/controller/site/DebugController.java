package com.springboot.jenka_coffee.controller.site;

import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.repository.AccountRepository;
import com.springboot.jenka_coffee.util.PasswordSecurity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/debug")
public class DebugController {

    private final AccountRepository accountRepository;
    private final PasswordSecurity passwordSecurity;

    public DebugController(AccountRepository accountRepository, PasswordSecurity passwordSecurity) {
        this.accountRepository = accountRepository;
        this.passwordSecurity = passwordSecurity;
    }

    /**
     * Debug endpoint: Check tài khoản có tồn tại và password hash
     * Usage: GET /debug/check-account?username=admin
     */
    @GetMapping("/check-account")
    public Map<String, Object> checkAccount(@RequestParam String username) {
        Map<String, Object> debug = new HashMap<>();

        Account account = accountRepository.findById(username).orElse(null);

        if (account == null) {
            debug.put("status", "NOT_FOUND");
            debug.put("message", "Tài khoản không tồn tại");
            return debug;
        }

        debug.put("status", "FOUND");
        debug.put("username", account.getUsername());
        debug.put("fullname", account.getFullname());
        debug.put("email", account.getEmail());
        debug.put("activated", account.getActivated());
        debug.put("admin", account.getAdmin());
        debug.put("passwordHash", account.getPasswordHash());
        debug.put("passwordHashLength", account.getPasswordHash() != null ? account.getPasswordHash().length() : 0);
        debug.put("isHashedPassword", passwordSecurity.isPasswordHashed(account.getPasswordHash()));

        return debug;
    }

    /**
     * Debug endpoint: Test password verification
     * Usage: GET /debug/test-password?username=admin&password=123
     */
    @GetMapping("/test-password")
    public Map<String, Object> testPassword(@RequestParam String username, @RequestParam String password) {
        Map<String, Object> debug = new HashMap<>();

        Account account = accountRepository.findById(username).orElse(null);

        if (account == null) {
            debug.put("status", "ERROR");
            debug.put("message", "Tài khoản không tồn tại");
            return debug;
        }

        String hash = account.getPasswordHash();
        boolean isMatch = passwordSecurity.verifyPassword(password, hash);

        debug.put("status", "OK");
        debug.put("username", username);
        debug.put("passwordInput", password);
        debug.put("passwordHash", hash);
        debug.put("hashLength", hash != null ? hash.length() : 0);
        debug.put("isHashedPassword", passwordSecurity.isPasswordHashed(hash));
        debug.put("passwordMatch", isMatch);
        debug.put("result", isMatch ? "✅ PASSWORD CORRECT" : "❌ PASSWORD WRONG");

        return debug;
    }
}
