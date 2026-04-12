package com.springboot.jenka_coffee.api;

import com.springboot.jenka_coffee.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * BUG-50 WARNING: Analytics Data Manipulation Risk
 * 
 * PROBLEM: Visitor ping endpoint is open (permitAll) with no authentication
 * - Endpoint: POST /api/visitors/ping
 * - No JWT token required, no session validation
 * - No fingerprinting or cryptographic proof of legitimacy
 * - Attacker can spam requests to inflate visitor counts
 * 
 * ATTACK SCENARIO:
 * 1. Competitor runs script: while(true) { fetch('/api/visitors/ping', {method:'POST'}) }
 * 2. Dashboard shows "1 BILLION VISITORS" - completely fake data
 * 3. Charts break, business decisions based on fake metrics
 * 4. Customer loses trust when they discover data can be manipulated
 * 
 * CURRENT MITIGATION:
 * 1. Rate limiting: 60 requests per 10 minutes per IP (RateLimitFilter)
 * 2. Unique client ID: IP + User-Agent combination
 * 3. Trusted proxy logic: Prevents IP spoofing via X-Forwarded-For
 * 4. 5-minute timeout: Users inactive for 5 minutes removed from online count
 * 5. Cleanup job: Removes expired users every minute
 * 
 * LIMITATIONS:
 * - Attacker with botnet (multiple IPs) can bypass rate limiting
 * - User-Agent can be spoofed easily
 * - No cryptographic proof that request came from legitimate frontend
 * - In-memory counters are volatile (lost on restart)
 * 
 * PRODUCTION RECOMMENDATIONS:
 * 1. Issue short-lived session token on first page load:
 *    - Generate JWT with 1-hour expiration
 *    - Store in HttpOnly cookie
 *    - Require token for all /ping requests
 *    - Prevents automated scripts without browser
 * 
 * 2. Implement device fingerprinting:
 *    - Canvas fingerprinting
 *    - WebGL fingerprinting
 *    - Browser feature detection
 *    - Combine with IP for unique ID
 * 
 * 3. Use Redis for distributed rate limiting:
 *    - Current Caffeine cache is per-instance
 *    - Redis works across load-balanced servers
 *    - Prevents attacker from hitting different servers
 * 
 * 4. Persist analytics to database:
 *    - Current counters reset on restart (volatile)
 *    - Save to DB every N minutes via @Scheduled
 *    - Use write-through cache pattern
 * 
 * 5. Implement anomaly detection:
 *    - Alert when visitor count spikes abnormally
 *    - Track request patterns (timing, frequency)
 *    - Flag suspicious IPs for manual review
 * 
 * 6. Add CAPTCHA for suspicious traffic:
 *    - If rate limit exceeded, require CAPTCHA
 *    - Prevents automated bots
 *    - Only affects suspicious users
 * 
 * RISK LEVEL: Medium (requires effort but possible with botnets)
 * BUSINESS IMPACT: High (fake metrics damage credibility and decisions)
 */
@RestController
@RequestMapping("/api/visitors")
public class ApiVisitorController {

    // VULN-VOLATILE-ANALYTICS: These counters are volatile (lost on restart)
    // TODO: Implement database persistence for production
    private final AtomicLong totalVisits  = new AtomicLong(0);
    private final AtomicLong todayVisits  = new AtomicLong(0);
    private final AtomicLong monthVisits  = new AtomicLong(0);
    
    // Track online users by IP + last activity timestamp
    private final ConcurrentHashMap<String, Long> onlineUsers = new ConcurrentHashMap<>();
    private static final long ONLINE_TIMEOUT_MS = 5 * 60 * 1000; // 5 minutes

    private LocalDate currentDay   = LocalDate.now();
    private int       currentMonth = LocalDate.now().getMonthValue();

    /**
     * POST /api/visitors/ping — gọi khi user load trang.
     * Tăng counter và trả về stats hiện tại.
     */
    @PostMapping("/ping")
    public ResponseEntity<ApiResponse<Map<String, Long>>> ping(HttpServletRequest request) {
        checkReset();
        
        String clientId = getClientIdentifier(request);
        long now = System.currentTimeMillis();
        
        // Chỉ tăng visit counter nếu là lần đầu trong ngày (dựa vào IP)
        Long lastVisit = onlineUsers.get(clientId);
        if (lastVisit == null || (now - lastVisit) > ONLINE_TIMEOUT_MS) {
            totalVisits.incrementAndGet();
            todayVisits.incrementAndGet();
            monthVisits.incrementAndGet();
        }
        
        // Update last activity timestamp
        onlineUsers.put(clientId, now);

        return ResponseEntity.ok(ApiResponse.success("OK", buildStats()));
    }

    /** GET /api/visitors/stats — lấy stats không tăng counter */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> stats() {
        return ResponseEntity.ok(ApiResponse.success("OK", buildStats()));
    }

    /** Giảm online count khi user rời trang (beacon API) */
    @PostMapping("/leave")
    public ResponseEntity<Void> leave(HttpServletRequest request) {
        String clientId = getClientIdentifier(request);
        onlineUsers.remove(clientId);
        return ResponseEntity.ok().build();
    }

    /** Reset today counter mỗi nửa đêm */
    @Scheduled(cron = "0 0 0 * * *")
    public void resetDaily() {
        todayVisits.set(0);
        currentDay = LocalDate.now();
    }

    /** Reset month counter đầu tháng */
    @Scheduled(cron = "0 0 0 1 * *")
    public void resetMonthly() {
        monthVisits.set(0);
        currentMonth = LocalDate.now().getMonthValue();
    }

    /** Cleanup inactive users mỗi 1 phút */
    @Scheduled(fixedDelay = 60_000)
    public void cleanupInactiveUsers() {
        long now = System.currentTimeMillis();
        onlineUsers.entrySet().removeIf(entry -> 
            (now - entry.getValue()) > ONLINE_TIMEOUT_MS
        );
    }

    private void checkReset() {
        LocalDate today = LocalDate.now();
        if (!today.equals(currentDay)) {
            todayVisits.set(0);
            currentDay = today;
        }
        if (today.getMonthValue() != currentMonth) {
            monthVisits.set(0);
            currentMonth = today.getMonthValue();
        }
    }

    private Map<String, Long> buildStats() {
        // Cleanup expired users before counting
        long now = System.currentTimeMillis();
        long activeCount = onlineUsers.values().stream()
            .filter(timestamp -> (now - timestamp) <= ONLINE_TIMEOUT_MS)
            .count();
        
        return Map.of(
                "online", Math.max(0, activeCount),
                "today",  todayVisits.get(),
                "month",  monthVisits.get(),
                "total",  totalVisits.get()
        );
    }
    
    /**
     * Get unique client identifier from IP + User-Agent + SessionId
     * Prevents same user from being counted multiple times
     * DEV-FIX: Use sessionId to track individual browser tabs correctly
     */
    private String getClientIdentifier(HttpServletRequest request) {
        String ip = getClientIp(request);
        
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) userAgent = "unknown";
        
        // Get or create session ID for this browser tab
        String sessionId = request.getSession(true).getId();
        
        // Combine IP + first 50 chars of user agent + sessionId for uniqueness
        // This ensures each browser tab is tracked separately but correctly
        return ip + "|" + (userAgent.length() > 50 ? userAgent.substring(0, 50) : userAgent) + "|" + sessionId;
    }
    
    /**
     * VULN-FAKE-ANALYTICS FIX: Only trust X-Forwarded-For from trusted proxies
     * Same logic as RateLimitFilter to prevent fake analytics
     */
    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (isTrustedProxy(remoteAddr)) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                return realIp;
            }
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
