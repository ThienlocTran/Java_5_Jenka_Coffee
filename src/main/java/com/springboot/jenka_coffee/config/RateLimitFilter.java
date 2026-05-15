package com.springboot.jenka_coffee.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Rate limiter per IP — dùng Caffeine Cache với TTL tự động.
 * VULN-H02 FIX: Thay ConcurrentHashMap + periodic clear bằng sliding window.
 * Caffeine tự evict entry sau khi không được access trong TTL → không reset toàn bộ.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // Caffeine cache: tự evict sau 5 phút không access — sliding window thực sự
    private final Cache<String, Bucket> loginBuckets   = buildCache(Duration.ofMinutes(5));
    private final Cache<String, Bucket> forgotBuckets  = buildCache(Duration.ofMinutes(5));
    private final Cache<String, Bucket> signupBuckets  = buildCache(Duration.ofMinutes(5));
    private final Cache<String, Bucket> otpBuckets     = buildCache(Duration.ofMinutes(5));
    private final Cache<String, Bucket> contactBuckets = buildCache(Duration.ofMinutes(35));
    private final Cache<String, Bucket> checkoutBuckets = buildCache(Duration.ofMinutes(15));
    // VULN-FAKE-TRAFFIC FIX: Rate limit cho visitor ping để ngăn fake traffic
    private final Cache<String, Bucket> visitorBuckets = buildCache(Duration.ofMinutes(10));
    // VULN-ENUMERATION FIX: Rate limit cho admin check-username/email để ngăn enumeration
    private final Cache<String, Bucket> adminCheckBuckets = buildCache(Duration.ofMinutes(5));
    // VULN-SMS-BOMBING FIX: Rate limit per phone number to prevent distributed SMS bombing
    // BUG-48 FIX: Email-specific rate limiting to prevent email spam DoS
    // Prevents attacker from bombing victim's email with password reset/activation emails
    // Limit: 1 email per minute per email address (prevents Gmail/SendGrid suspension)
    private final Cache<String, Bucket> emailBuckets = buildCache(Duration.ofMinutes(60));
    // VULN-H03 FIX: Rate limit for /api/auth/me to prevent DB spam
    // Limit: 60 requests per minute per IP (prevents DB query flooding with stolen token)
    private final Cache<String, Bucket> authMeBuckets = buildCache(Duration.ofMinutes(1));
    // VULN-RATE-LIMIT-GAPS FIX: Rate limit for public product search to prevent DB DoS
    // Limit: 100 requests per minute per IP (prevents expensive search/filter/sort queries)
    private final Cache<String, Bucket> productBuckets = buildCache(Duration.ofMinutes(1));
    // VULN #16 FIX: Rate limit for feedback submission to prevent storage DoS
    // Limit: 2 feedbacks per hour per IP (prevents database/storage spam)
    private final Cache<String, Bucket> feedbackBuckets = buildCache(Duration.ofHours(1));

    private Cache<String, Bucket> buildCache(Duration ttl) {
        return Caffeine.newBuilder()
                .expireAfterAccess(ttl)
                .maximumSize(100_000) // giới hạn tổng entries tránh OOM
                .build();
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req,
                                    @NonNull HttpServletResponse res,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String path   = req.getRequestURI();
        String method = req.getMethod();

        String ip     = getClientIp(req);
        Bucket bucket = null;

        // Rate limit cho POST endpoints
        if ("POST".equalsIgnoreCase(method)) {
            if (path.startsWith("/api/auth/login")) {
                bucket = loginBuckets.get(ip, k -> buildBucket(10, Duration.ofMinutes(1)));
            } else if (path.startsWith("/api/auth/forgot-password")) {
                // BUG-48 FIX: Email-specific rate limiting for forgot-password
                // Extract email from request parameter (not body to avoid consumption issue)
                String email = req.getParameter("email");
                if (email != null && !email.isBlank()) {
                    // Rate limit by email: 1 request per minute per email address
                    Bucket emailBucket = emailBuckets.get(email.toLowerCase(), 
                        k -> buildBucket(1, Duration.ofMinutes(1)));
                    if (!emailBucket.tryConsume(1)) {
                        res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        res.setCharacterEncoding("UTF-8");
                        res.getWriter().write("{\"status\":\"ERROR\",\"message\":\"Vui lòng đợi 1 phút trước khi gửi lại yêu cầu.\"}");
                        return;
                    }
                }
                // Also rate limit by IP as fallback
                bucket = forgotBuckets.get(ip, k -> buildBucket(5, Duration.ofMinutes(1)));
            } else if (path.startsWith("/api/auth/resend-activation")) {
                // BUG-48 FIX: Email-specific rate limiting for resend-activation
                String email = req.getParameter("email");
                if (email != null && !email.isBlank()) {
                    Bucket emailBucket = emailBuckets.get(email.toLowerCase(), 
                        k -> buildBucket(1, Duration.ofMinutes(1)));
                    if (!emailBucket.tryConsume(1)) {
                        res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        res.setCharacterEncoding("UTF-8");
                        res.getWriter().write("{\"status\":\"ERROR\",\"message\":\"Vui lòng đợi 1 phút trước khi gửi lại yêu cầu.\"}");
                        return;
                    }
                }
                bucket = signupBuckets.get(ip, k -> buildBucket(5, Duration.ofMinutes(1)));
            } else if (path.startsWith("/api/auth/signup")) {
                // VULN-REQUEST-BODY-CONSUMPTION FIX: Disabled phone extraction from body
                // Reading body in filter breaks Spring @RequestBody parsing
                // Use IP-based rate limiting only (5 signups per minute per IP)
                bucket = signupBuckets.get(ip, k -> buildBucket(5, Duration.ofMinutes(1)));
            } else if (path.startsWith("/api/auth/verify-otp")) {
                // VULN-064 FIX: 10 req/5 phút — chặt hơn để ngăn burst brute force OTP
                bucket = otpBuckets.get(ip, k -> buildBucket(10, Duration.ofMinutes(5)));
            } else if (path.startsWith("/api/auth/resend-otp")) {
                // VULN-REQUEST-BODY-CONSUMPTION FIX: Disabled phone extraction from body
                // Use IP-based rate limiting only (5 resends per minute per IP)
                bucket = signupBuckets.get(ip, k -> buildBucket(5, Duration.ofMinutes(1)));
            } else if (path.startsWith("/api/contact/send")) {
                // BUG-48 FIX: Email-specific rate limiting for contact form
                String email = req.getParameter("email");
                if (email != null && !email.isBlank()) {
                    Bucket emailBucket = emailBuckets.get(email.toLowerCase(), 
                        k -> buildBucket(2, Duration.ofMinutes(30)));
                    if (!emailBucket.tryConsume(1)) {
                        res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        res.setCharacterEncoding("UTF-8");
                        res.getWriter().write("{\"status\":\"ERROR\",\"message\":\"Vui lòng đợi 30 phút trước khi gửi lại tin nhắn.\"}");
                        return;
                    }
                }
                bucket = contactBuckets.get(ip, k -> buildBucket(5, Duration.ofMinutes(30)));
            } else if (path.startsWith("/api/orders/checkout")) {
                // VULN-ORDER-FLOODING FIX: Rate limit checkout to prevent spam orders
                // Limit: 3 checkouts per 10 minutes per IP
                // Prevents: fake orders, email spam, inventory manipulation
                bucket = checkoutBuckets.get(ip, k -> buildBucket(3, Duration.ofMinutes(10)));
            } else if (path.startsWith("/api/feedbacks")) {
                // VULN #16 FIX: Rate limit feedback submission to prevent storage DoS
                // Limit: 2 feedbacks per hour per IP
                // Prevents: database spam, storage exhaustion, disk full
                bucket = feedbackBuckets.get(ip, k -> buildBucket(2, Duration.ofHours(1)));
            }
        }
        
        // VULN-FAKE-TRAFFIC FIX: Rate limit cho visitor ping - 60 requests/10 phút
        if (path.startsWith("/api/visitors/ping")) {
            bucket = visitorBuckets.get(ip, k -> buildBucket(60, Duration.ofMinutes(10)));
        }
        
        // VULN-ENUMERATION FIX: Rate limit cho admin check endpoints - 30 requests/5 phút
        if ("GET".equalsIgnoreCase(method) && 
            (path.startsWith("/api/admin/accounts/check-username") || 
             path.startsWith("/api/admin/accounts/check-email"))) {
            bucket = adminCheckBuckets.get(ip, k -> buildBucket(30, Duration.ofMinutes(5)));
        }
        
        // VULN-H03 FIX: Rate limit cho /api/auth/me - 60 requests/phút
        // Prevents DB spam attack with stolen token
        if ("GET".equalsIgnoreCase(method) && path.startsWith("/api/auth/me")) {
            bucket = authMeBuckets.get(ip, k -> buildBucket(60, Duration.ofMinutes(1)));
        }
        
        // VULN-RATE-LIMIT-GAPS FIX: Rate limit cho /api/products - 100 requests/phút
        // Prevents DB DoS via expensive search/filter/sort queries
        if ("GET".equalsIgnoreCase(method) && path.startsWith("/api/products")) {
            bucket = productBuckets.get(ip, k -> buildBucket(100, Duration.ofMinutes(1)));
        }

        if (bucket != null && !bucket.tryConsume(1)) {
            res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            res.setCharacterEncoding("UTF-8");
            res.getWriter().write("{\"status\":\"ERROR\",\"message\":\"Quá nhiều yêu cầu. Vui lòng thử lại sau.\"}");
            return;
        }

        chain.doFilter(req, res);
    }

    private Bucket buildBucket(int capacity, Duration refillDuration) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillIntervally(capacity, refillDuration)
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    /** VULN-002 FIX: Chỉ trust X-Forwarded-For khi đến từ trusted proxy */
    private String getClientIp(HttpServletRequest req) {
        String remoteAddr = req.getRemoteAddr();
        if (isTrustedProxy(remoteAddr)) {
            String forwarded = req.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
            String realIp = req.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) return realIp;
        }
        return remoteAddr;
    }

    /**
     * Check if IP belongs to trusted private network (internal proxy/load-balancer).
     * Only trust X-Forwarded-For headers from these sources.
     *
     * RFC 1918 private ranges:
     *   10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16
     * Localhost: 127.0.0.1, ::1
     */
    private boolean isTrustedProxy(String ip) {
        if (ip == null) return false;
        return ip.startsWith("10.")
                || ip.startsWith("172.16.") || ip.startsWith("172.17.")
                || ip.startsWith("172.18.") || ip.startsWith("172.19.")
                || ip.startsWith("172.20.") || ip.startsWith("172.21.")
                || ip.startsWith("172.22.") || ip.startsWith("172.23.")
                || ip.startsWith("172.24.") || ip.startsWith("172.25.")
                || ip.startsWith("172.26.") || ip.startsWith("172.27.")
                || ip.startsWith("172.28.") || ip.startsWith("172.29.")
                || ip.startsWith("172.30.") || ip.startsWith("172.31.")
                || ip.startsWith("192.168.")
                || "127.0.0.1".equals(ip)
                || "::1".equals(ip);
    }
    
    /*
     * VULN-SMS-BOMBING FIX: Extract phone number from request body
     * Used to rate limit by phone number instead of just IP
     * VULN-REQUEST-BODY-CONSUMPTION FIX: DO NOT read request body in filter!
     * Reading req.getReader() consumes the input stream, making it unavailable
     * for controllers. This breaks @RequestBody parsing in Spring.
     * Solution: Use request parameters or headers for rate limiting instead.
     * For phone-based rate limiting, we'll rely on IP-based limits only.
     * If phone-specific limits are critical, implement using:
     * 1. Custom wrapper that caches request body
     * 2. Rate limit in controller layer after body is parsed
     * 3. Separate rate limit service called from controller
     */

}
