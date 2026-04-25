package com.springboot.jenka_coffee.api;

import com.springboot.jenka_coffee.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

// BUG-50 WARNING: Analytics Data Manipulation Risk
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
    
    // FIX: Track unique visitors per day/month to prevent duplicate counting
    private final ConcurrentHashMap<String, LocalDate> dailyVisitors = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> monthlyVisitors = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> totalVisitors = new ConcurrentHashMap<>();

    private LocalDate currentDay   = LocalDate.now();
    private int       currentMonth = LocalDate.now().getMonthValue();

    /**
     * POST /api/visitors/ping — gọi khi user load trang.
     * Tăng counter và trả về stats hiện tại.
     * 
     * FIX: Prevent duplicate counting
     * - Total: Count once per unique visitor (lifetime)
     * - Today: Count once per unique visitor per day
     * - Month: Count once per unique visitor per month
     * - Online: Update activity timestamp
     */
    @PostMapping("/ping")
    public ResponseEntity<ApiResponse<Map<String, Long>>> ping(HttpServletRequest request) {
        checkReset();
        
        String clientId = getClientIdentifier(request);
        long now = System.currentTimeMillis();
        LocalDate today = LocalDate.now();
        int currentMonthValue = today.getMonthValue();
        
        // 1. Update online status (always)
        onlineUsers.put(clientId, now);
        
        // 2. Count TOTAL visits (once per unique visitor, lifetime)
        if (!totalVisitors.containsKey(clientId)) {
            totalVisits.incrementAndGet();
            totalVisitors.put(clientId, true);
        }
        
        // 3. Count TODAY visits (once per unique visitor per day)
        LocalDate lastDailyVisit = dailyVisitors.get(clientId);
        if (lastDailyVisit == null || !lastDailyVisit.equals(today)) {
            todayVisits.incrementAndGet();
            dailyVisitors.put(clientId, today);
        }
        
        // 4. Count MONTH visits (once per unique visitor per month)
        Integer lastMonthVisit = monthlyVisitors.get(clientId);
        if (lastMonthVisit == null || lastMonthVisit != currentMonthValue) {
            monthVisits.incrementAndGet();
            monthlyVisitors.put(clientId, currentMonthValue);
        }

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
        dailyVisitors.clear(); // Clear daily tracking map
        currentDay = LocalDate.now();
    }

    /** Reset month counter đầu tháng */
    @Scheduled(cron = "0 0 0 1 * *")
    public void resetMonthly() {
        monthVisits.set(0);
        monthlyVisitors.clear(); // Clear monthly tracking map
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
            dailyVisitors.clear(); // Clear daily tracking
            currentDay = today;
        }
        if (today.getMonthValue() != currentMonth) {
            monthVisits.set(0);
            monthlyVisitors.clear(); // Clear monthly tracking
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
                "online", activeCount,
                "today",  todayVisits.get(),
                "month",  monthVisits.get(),
                "total",  totalVisits.get()
        );
    }
    
    /**
     * Get unique client identifier from IP + User-Agent
     * Prevents same user from being counted multiple times
     * FIX: Removed session dependency since app uses STATELESS session policy
     */
    private String getClientIdentifier(HttpServletRequest request) {
        String ip = getClientIp(request);
        
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) userAgent = "unknown";
        
        // Combine IP + first 50 chars of user agent for uniqueness
        // Note: This means multiple tabs from same IP will be counted as one user
        return ip + "|" + (userAgent.length() > 50 ? userAgent.substring(0, 50) : userAgent);
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
        return checkIp(ip);
    }

    public static boolean checkIp(String ip) {
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
