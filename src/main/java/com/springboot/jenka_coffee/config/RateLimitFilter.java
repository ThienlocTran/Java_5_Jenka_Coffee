package com.springboot.jenka_coffee.config;

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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter per IP cho các endpoint nhạy cảm.
 * Dùng Bucket4j in-memory — không cần Redis.
 *
 * Giới hạn:
 *   - /api/auth/login          : 10 req/phút per IP  (chống brute force)
 *   - /api/auth/forgot-password: 5  req/phút per IP  (chống spam email)
 *   - /api/auth/signup         : 5  req/phút per IP  (chống tạo tài khoản hàng loạt)
 *   - /api/auth/verify-otp     : 5  req/phút per IP  (chống brute force OTP)
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // Map<IP, Bucket> — tự cleanup khi bucket hết token và không được dùng
    private final Map<String, Bucket> loginBuckets        = new ConcurrentHashMap<>();
    private final Map<String, Bucket> forgotBuckets       = new ConcurrentHashMap<>();
    private final Map<String, Bucket> signupBuckets       = new ConcurrentHashMap<>();
    private final Map<String, Bucket> otpBuckets          = new ConcurrentHashMap<>();
    // VULN-043 FIX: Rate limit cho booking và contact — ngăn email bomb
    private final Map<String, Bucket> bookingBuckets      = new ConcurrentHashMap<>();
    private final Map<String, Bucket> contactBuckets      = new ConcurrentHashMap<>();

    // Dọn dẹp bucket cũ mỗi 10 phút để tránh map phình to
    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 600_000)
    public void evictOldBuckets() {
        loginBuckets.clear();
        forgotBuckets.clear();
        signupBuckets.clear();
        otpBuckets.clear();
        bookingBuckets.clear();
        contactBuckets.clear();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String path = req.getRequestURI();
        String method = req.getMethod();

        if (!"POST".equalsIgnoreCase(method)) {
            chain.doFilter(req, res);
            return;
        }

        String ip = getClientIp(req);
        Bucket bucket = null;

        if (path.startsWith("/api/auth/login")) {
            bucket = loginBuckets.computeIfAbsent(ip, k -> buildBucket(10, Duration.ofMinutes(1)));
        } else if (path.startsWith("/api/auth/forgot-password")) {
            bucket = forgotBuckets.computeIfAbsent(ip, k -> buildBucket(5, Duration.ofMinutes(1)));
        } else if (path.startsWith("/api/auth/signup")) {
            bucket = signupBuckets.computeIfAbsent(ip, k -> buildBucket(5, Duration.ofMinutes(1)));
        } else if (path.startsWith("/api/auth/verify-otp")) {
            // VULN-064 FIX: 10 req/5 phút — chặt hơn để ngăn burst brute force OTP
            bucket = otpBuckets.computeIfAbsent(ip, k -> buildBucket(10, Duration.ofMinutes(5)));
        } else if (path.startsWith("/api/booking/submit")) {
            // VULN-043 FIX: 3 booking per 10 phút per IP — ngăn email bomb
            bucket = bookingBuckets.computeIfAbsent(ip, k -> buildBucket(3, Duration.ofMinutes(10)));
        } else if (path.startsWith("/api/contact/send")) {
            // VULN-043 FIX: 5 contact per 30 phút per IP
            bucket = contactBuckets.computeIfAbsent(ip, k -> buildBucket(5, Duration.ofMinutes(30)));
        }

        if (bucket != null && !bucket.tryConsume(1)) {
            res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            res.setCharacterEncoding("UTF-8");
            res.getWriter().write("{\"status\":\"ERROR\",\"message\":\"Quá nhiều yêu cầu. Vui lòng thử lại sau 1 phút.\"}");
            return;
        }

        chain.doFilter(req, res);
    }

    private Bucket buildBucket(int capacity, Duration refillDuration) {
        Bandwidth limit = Bandwidth.classic(capacity,
                Refill.intervally(capacity, refillDuration));
        return Bucket.builder().addLimit(limit).build();
    }

    /** VULN-002 FIX: Chỉ trust X-Forwarded-For khi request đến từ trusted proxy IP.
     *  Ngăn attacker spoof header để bypass rate limit. */
    private String getClientIp(HttpServletRequest req) {
        String remoteAddr = req.getRemoteAddr();

        // Chỉ tin X-Forwarded-For nếu request đến từ trusted proxy (Railway/Vercel load balancer)
        if (isTrustedProxy(remoteAddr)) {
            String forwarded = req.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            String realIp = req.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) return realIp;
        }

        return remoteAddr;
    }

    /**
     * Whitelist IP ranges của Railway/Vercel internal load balancers.
     * Các request từ internet trực tiếp sẽ dùng remoteAddr thật.
     */
    private boolean isTrustedProxy(String ip) {
        if (ip == null) return false;
        // Private network ranges (RFC 1918) — internal load balancers
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
