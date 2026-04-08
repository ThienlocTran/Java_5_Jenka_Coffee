package com.springboot.jenka_coffee.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    private final Cache<String, Bucket> bookingBuckets = buildCache(Duration.ofMinutes(15));
    private final Cache<String, Bucket> contactBuckets = buildCache(Duration.ofMinutes(35));

    private Cache<String, Bucket> buildCache(Duration ttl) {
        return Caffeine.newBuilder()
                .expireAfterAccess(ttl)
                .maximumSize(100_000) // giới hạn tổng entries tránh OOM
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String path   = req.getRequestURI();
        String method = req.getMethod();

        if (!"POST".equalsIgnoreCase(method)) {
            chain.doFilter(req, res);
            return;
        }

        String ip     = getClientIp(req);
        Bucket bucket = null;

        if (path.startsWith("/api/auth/login")) {
            bucket = loginBuckets.get(ip, k -> buildBucket(10, Duration.ofMinutes(1)));
        } else if (path.startsWith("/api/auth/forgot-password")) {
            bucket = forgotBuckets.get(ip, k -> buildBucket(5, Duration.ofMinutes(1)));
        } else if (path.startsWith("/api/auth/signup")) {
            bucket = signupBuckets.get(ip, k -> buildBucket(5, Duration.ofMinutes(1)));
        } else if (path.startsWith("/api/auth/verify-otp")) {
            // VULN-064 FIX: 10 req/5 phút — chặt hơn để ngăn burst brute force OTP
            bucket = otpBuckets.get(ip, k -> buildBucket(10, Duration.ofMinutes(5)));
        } else if (path.startsWith("/api/booking/submit")) {
            bucket = bookingBuckets.get(ip, k -> buildBucket(3, Duration.ofMinutes(10)));
        } else if (path.startsWith("/api/contact/send")) {
            bucket = contactBuckets.get(ip, k -> buildBucket(5, Duration.ofMinutes(30)));
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
        Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(capacity, refillDuration));
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
                || "127.0.0.1".equals(ip) || "::1".equals(ip);
    }
}
