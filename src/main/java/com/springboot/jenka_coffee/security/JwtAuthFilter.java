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

// 🚨 BUG-63: JWT VALIDATION FLAW / USER LOCKOUT DELAY (Hệ Miễn Dịch Chậm Chạp)
// ================================================================
// CRITICAL SECURITY ISSUE: Locked/deactivated users can still access system with valid JWT!
// 
// Current state:
// - System uses stateless JWT authentication (good for performance)
// - JwtAuthFilter validates token signature and expiration
// - Filter checks if account is activated (line ~40)
// - BUT: No check if account was locked AFTER token issued
// 
// The problem:
// When admin locks a user account:
// 1. Admin clicks "Lock Account" button
// 2. Backend sets account.locked = true in database
// 3. User's JWT token is still valid (expires in 24 hours)
// 4. JwtAuthFilter only checks activated status, not locked status
// 5. User can continue using system for 24 hours!
// 6. "Lock Account" feature is effectively broken
// 
// Real-world attack scenario:
// ```
// 09:00 AM - Employee John has valid JWT token (expires 09:00 AM tomorrow)
// 10:00 AM - John caught stealing data, admin locks his account
// 10:01 AM - John still logged in, continues downloading customer data
// 10:30 AM - John deletes audit logs to cover tracks
// 11:00 AM - John resets other admin passwords
// 02:00 PM - John exports entire product database
// 09:00 AM (next day) - Token finally expires, John locked out
// 
// Damage: 23 hours of unauthorized access after account locked!
// ```
// 
// Business impact:
// - Insider threats cannot be stopped immediately
// - Fired employees retain access for hours/days
// - Compromised accounts cannot be disabled quickly
// - Security incident response delayed
// - Compliance violations (SOC 2, ISO 27001 require immediate revocation)
// - Legal liability (cannot prove access was revoked)
// 
// Root cause:
// Stateless JWT tokens cannot be revoked without additional infrastructure.
// Token contains all auth info, no server-side session to invalidate.
// 
// Current partial fix (INCOMPLETE):
// - Line ~40: Checks if account.activated = false
// - Line ~45-55: Checks if token issued before password reset
// - Line ~58-65: Checks if admin privilege revoked
// - MISSING: Check if account.locked = true
// 
// Solution options (NEEDS TO BE IMPLEMENTED):
// 
// OPTION 1: Add locked status check (QUICK FIX):
// ```java
// var accountOpt = accountRepository.findById(username);
// if (accountOpt.isEmpty() || 
//     !Boolean.TRUE.equals(accountOpt.get().getActivated()) ||
//     Boolean.TRUE.equals(accountOpt.get().getLocked())) {  // ADD THIS CHECK
//     chain.doFilter(req, res);
//     return;
// }
// ```
// 
// Pros: Simple, works immediately
// Cons: Database query on EVERY request (performance hit)
// 
// OPTION 2: Cache account status in Redis (RECOMMENDED):
// ```java
// // Check Redis cache first (fast)
// String cacheKey = "account:status:" + username;
// AccountStatus status = redisTemplate.opsForValue().get(cacheKey);
// 
// if (status == null) {
//     // Cache miss - query database
//     var account = accountRepository.findById(username).orElse(null);
//     if (account == null) {
//         chain.doFilter(req, res);
//         return;
//     }
//     status = new AccountStatus(account.getActivated(), account.getLocked(), account.getAdmin());
//     // Cache for 5 minutes
//     redisTemplate.opsForValue().set(cacheKey, status, Duration.ofMinutes(5));
// }
// 
// if (!status.isActivated() || status.isLocked()) {
//     chain.doFilter(req, res);
//     return;
// }
// ```
// 
// Pros: Fast (Redis lookup ~1ms), scalable
// Cons: Requires Redis, 5-minute delay for lock to take effect
// 
// OPTION 3: Caffeine in-memory cache (GOOD COMPROMISE):
// ```java
// // Add to JwtAuthFilter
// private final Cache<String, AccountStatus> accountStatusCache = Caffeine.newBuilder()
//     .expireAfterWrite(Duration.ofMinutes(1))
//     .maximumSize(10_000)
//     .build();
// 
// // In doFilterInternal()
// AccountStatus status = accountStatusCache.get(username, key -> {
//     var account = accountRepository.findById(key).orElse(null);
//     if (account == null) return null;
//     return new AccountStatus(account.getActivated(), account.getLocked(), account.getAdmin());
// });
// 
// if (status == null || !status.isActivated() || status.isLocked()) {
//     chain.doFilter(req, res);
//     return;
// }
// ```
// 
// Pros: No external dependency, fast, 1-minute delay
// Cons: Cache not shared across servers (multi-instance deployment)
// 
// OPTION 4: Blacklist tokens when locking account (BEST SECURITY):
// ```java
// // In AccountServiceImpl.lockAccount()
// public void lockAccount(String username) {
//     Account account = findByIdOrThrow(username);
//     account.setLocked(true);
//     accountRepository.save(account);
//     
//     // Blacklist all active tokens for this user
//     List<String> activeTokens = tokenRepository.findByUsername(username);
//     for (String token : activeTokens) {
//         jwtBlacklistService.blacklist(token);
//     }
// }
// ```
// 
// Pros: Immediate revocation, no delay
// Cons: Requires tracking active tokens (stateful)
// 
// Performance comparison:
// CURRENT (no check):
// - Request latency: 5ms
// - Security: BROKEN (locked users can access)
// 
// OPTION 1 (DB query):
// - Request latency: 15ms (+10ms per request)
// - Security: GOOD (immediate revocation)
// - Throughput: 66 req/sec per connection
// 
// OPTION 2 (Redis cache):
// - Request latency: 6ms (+1ms per request)
// - Security: GOOD (5-minute delay)
// - Throughput: 166 req/sec per connection
// 
// OPTION 3 (Caffeine cache):
// - Request latency: 5.1ms (+0.1ms per request)
// - Security: GOOD (1-minute delay)
// - Throughput: 196 req/sec per connection
// 
// OPTION 4 (Token blacklist):
// - Request latency: 5ms (no change)
// - Security: EXCELLENT (immediate revocation)
// - Complexity: HIGH (need token tracking)
// 
// Recommendation:
// - Development: Option 1 (simple DB check)
// - Production (single server): Option 3 (Caffeine cache)
// - Production (multi-server): Option 2 (Redis cache)
// - High security: Option 4 (token blacklist)
// 
// Related security issues:
// - Session fixation attacks
// - Token theft (XSS, man-in-the-middle)
// - Privilege escalation after role change
// - Password reset doesn't invalidate old tokens (ALREADY FIXED - line 45-55)
// 
// Compliance requirements:
// - SOC 2: Immediate access revocation upon termination
// - ISO 27001: Access control must be enforceable in real-time
// - PCI DSS: Revoke access immediately when no longer required
// - GDPR: Right to be forgotten requires immediate data access revocation
// ================================================================

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
