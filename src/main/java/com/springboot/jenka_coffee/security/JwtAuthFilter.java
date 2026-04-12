package com.springboot.jenka_coffee.security;

import com.springboot.jenka_coffee.repository.AccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AccountRepository accountRepository;
    private final com.springboot.jenka_coffee.service.JwtBlacklistService jwtBlacklistService;

    public JwtAuthFilter(JwtService jwtService, AccountRepository accountRepository,
                        com.springboot.jenka_coffee.service.JwtBlacklistService jwtBlacklistService) {
        this.jwtService = jwtService;
        this.accountRepository = accountRepository;
        this.jwtBlacklistService = jwtBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String token = extractToken(req);

        if (token != null && jwtService.isValid(token) && jwtService.isAccessToken(token)) {
            // VULN-TOKEN-001 FIX: Kiểm tra token có bị blacklist không
            if (jwtBlacklistService.isBlacklisted(token)) {
                chain.doFilter(req, res);
                return;
            }
            
            String username = jwtService.extractUsername(token);
            
            // VULN-PRIVILEGE-REVOCATION FIX: Load account từ DB để verify admin status
            // Không tin JWT claim vì có thể đã bị revoke
            var accountOpt = accountRepository.findById(username);
            if (accountOpt.isEmpty() || !Boolean.TRUE.equals(accountOpt.get().getActivated())) {
                chain.doFilter(req, res);
                return;
            }
            
            var account = accountOpt.get();
            
            // VULN-SESSION-REVOCATION FIX: Check if token was issued before password reset
            if (account.getLastPasswordResetDate() != null) {
                long tokenIssuedAt = jwtService.extractIssuedAt(token);
                long passwordResetTime = account.getLastPasswordResetDate()
                        .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                
                if (tokenIssuedAt < passwordResetTime) {
                    // Token issued before password reset - reject
                    chain.doFilter(req, res);
                    return;
                }
            }
            
            // Lấy admin status từ DB (fresh data), không từ JWT token
            boolean isAdminInDb = Boolean.TRUE.equals(account.getAdmin());
            boolean isAdminInToken = jwtService.isAdmin(token);
            
            // VULN-PRIVILEGE-REVOCATION FIX: Nếu token claim admin=true nhưng DB là false → reject
            if (isAdminInToken && !isAdminInDb) {
                // Token đã stale, admin privilege đã bị revoke
                chain.doFilter(req, res);
                return;
            }

            List<SimpleGrantedAuthority> authorities = isAdminInDb
                    ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER"))
                    : List.of(new SimpleGrantedAuthority("ROLE_USER"));

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        chain.doFilter(req, res);
    }

    private String extractToken(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        if (req.getCookies() != null) {
            for (Cookie c : req.getCookies()) {
                if ("access_token".equals(c.getName())) return c.getValue();
            }
        }
        return null;
    }
}
