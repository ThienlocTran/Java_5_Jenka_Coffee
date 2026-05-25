package com.springboot.jenka_coffee.security;

import com.springboot.jenka_coffee.service.AccountService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// BUG-63: JWT VALIDATION FLAW / USER LOCKOUT DELAY (Hệ Miễn Dịch Chậm Chạp)
@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AccountService accountService;
    private final com.springboot.jenka_coffee.service.JwtBlacklistService jwtBlacklistService;

    public JwtAuthFilter(JwtService jwtService, AccountService accountService,
                        com.springboot.jenka_coffee.service.JwtBlacklistService jwtBlacklistService) {
        this.jwtService = jwtService;
        this.accountService = accountService;
        this.jwtBlacklistService = jwtBlacklistService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "/api/auth/google-login".equals(path)
                || path.startsWith("/api/visitors/");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req,
                                    @NonNull HttpServletResponse res,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String token = extractToken(req);

        if (token != null && jwtService.isValid(token) && jwtService.isAccessToken(token)) {
            // VULN-TOKEN-001 FIX: Kiểm tra token có bị blacklist không
            if (jwtBlacklistService.isBlacklisted(token)) {
                chain.doFilter(req, res);
                return;
            }

            String username = jwtService.extractUsername(token);

            AccountService.AccountSecurityInfo securityInfo;
            try {
                securityInfo = accountService.getAccountSecurityInfo(username);
            } catch (Exception e) {
                log.error("[Security] Failed to load account info for '{}': {}", username, e.getMessage());
                chain.doFilter(req, res);
                return;
            }

            if (!securityInfo.exists() || !securityInfo.activated()) {
                chain.doFilter(req, res);
                return;
            }

            // VULN-SESSION-REVOCATION FIX: Check if token was issued before password reset
            if (securityInfo.lastPasswordResetTimestamp() != null) {
                long tokenIssuedAt = jwtService.extractIssuedAt(token);
                long passwordResetTime = securityInfo.lastPasswordResetTimestamp();

                if (tokenIssuedAt < passwordResetTime) {
                    log.warn("[Security] Stale token rejected for user '{}' (issued before password reset)", username);
                    chain.doFilter(req, res);
                    return;
                }
            }

            boolean isAdminInDb    = securityInfo.admin();
            boolean isAdminInToken = jwtService.isAdmin(token);

            // VULN-PRIVILEGE-REVOCATION FIX: Nếu token claim admin=true nhưng DB là false → reject
            if (isAdminInToken && !isAdminInDb) {
                log.warn("[Security] Admin privilege revoked — token rejected for user '{}'", username);
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
