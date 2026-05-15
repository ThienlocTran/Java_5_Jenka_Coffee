package com.springboot.jenka_coffee.api;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.entity.VisitorStats;
import com.springboot.jenka_coffee.repository.VisitorStatsRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Visitor tracking with DB persistence.
 * - Unique daily visitors tracked in visitor_stats table → survives restart.
 * - "Online now" count still in-memory (ephemeral by design — 5-min window).
 *   This is acceptable: online count resets to 0 on restart (normal behavior).
 */
@Slf4j
@RestController
@RequestMapping("/api/visitors")
public class ApiVisitorController {

    private final VisitorStatsRepository visitorStatsRepository;
    private static final ZoneId STATS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    // Online tracking stays in-memory (ephemeral, 5-min window — intentional)
    private final ConcurrentHashMap<String, Long> onlineUsers = new ConcurrentHashMap<>();
    private static final long ONLINE_TIMEOUT_MS = 5 * 60 * 1000L; // 5 minutes

    // Deduplicate unique visitors within the current day (in-memory — fast)
    // This resets on restart, meaning a visitor on day N might be counted again after restart.
    // Acceptable for MVP — the daily total in DB is the source of truth.
    private final ConcurrentHashMap<String, LocalDate> seenTodayMap = new ConcurrentHashMap<>();

    public ApiVisitorController(VisitorStatsRepository visitorStatsRepository) {
        this.visitorStatsRepository = visitorStatsRepository;
    }

    /**
     * POST /api/visitors/ping — gọi khi user load trang.
     * Tăng counter trong DB và trả về stats hiện tại.
     */
    @PostMapping("/ping")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ping(HttpServletRequest request) {
        String clientId = getClientIdentifier(request);
        long now = System.currentTimeMillis();
        LocalDate today = LocalDate.now(STATS_ZONE);

        // 1. Update online status
        onlineUsers.put(clientId, now);

        // 2. Determine if this is a new unique visitor today
        LocalDate lastSeen = seenTodayMap.get(clientId);
        boolean isNewUniqueToday = (lastSeen == null || !lastSeen.equals(today));
        if (isNewUniqueToday) {
            seenTodayMap.put(clientId, today);
        }

        // 3. Count active online users
        int onlineCount = (int) onlineUsers.values().stream()
                .filter(ts -> (now - ts) <= ONLINE_TIMEOUT_MS)
                .count();

        // 4. Persist to DB via UPSERT (atomic increment)
        try {
            visitorStatsRepository.upsertVisit(today, isNewUniqueToday ? 1 : 0, onlineCount);
        } catch (Exception e) {
            // Non-critical — don't break the user's page load because of analytics
            log.warn("[VISITOR] Failed to persist visit stats: {}", e.getMessage());
        }

        return ResponseEntity.ok(ApiResponse.success("OK", buildStats(today, onlineCount)));
    }

    /** GET /api/visitors/stats — lấy stats hiện tại */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stats() {
        LocalDate today = LocalDate.now(STATS_ZONE);
        long now = System.currentTimeMillis();
        int onlineCount = (int) onlineUsers.values().stream()
                .filter(ts -> (now - ts) <= ONLINE_TIMEOUT_MS)
                .count();
        return ResponseEntity.ok(ApiResponse.success("OK", buildStats(today, onlineCount)));
    }

    /** POST /api/visitors/leave — user rời trang */
    @PostMapping("/leave")
    public ResponseEntity<Void> leave(HttpServletRequest request) {
        String clientId = getClientIdentifier(request);
        onlineUsers.remove(clientId);
        return ResponseEntity.ok().build();
    }

    /** Cleanup online map mỗi 1 phút */
    @Scheduled(fixedDelay = 60_000)
    public void cleanupInactiveUsers() {
        long now = System.currentTimeMillis();
        onlineUsers.entrySet().removeIf(e -> (now - e.getValue()) > ONLINE_TIMEOUT_MS);
    }

    /** Cleanup seenTodayMap sau nửa đêm để không giữ data cũ */
    @Scheduled(cron = "0 5 0 * * *") // 00:05 hàng ngày
    public void resetDailyDedup() {
        seenTodayMap.clear();
        log.debug("[VISITOR] Daily dedup map cleared");
    }

    private Map<String, Object> buildStats(LocalDate today, int onlineCount) {
        VisitorStats todayStats = visitorStatsRepository.findByStatDate(today).orElse(null);

        // Use unique visitors consistently for the public visitor counter.
        long todayVisits = todayStats != null ? todayStats.getUniqueVisitors() : 0;
        long totalVisits = 0;
        long monthVisits = 0;

        // Calculate month total from DB
        LocalDate monthStart = today.withDayOfMonth(1);
        try {
            totalVisits = visitorStatsRepository.sumUniqueVisitors();
            monthVisits = visitorStatsRepository.sumUniqueVisitorsSince(monthStart);
        } catch (Exception e) {
            log.warn("[VISITOR] Failed to read stats from DB: {}", e.getMessage());
        }

        return Map.of(
                "online", (long) onlineCount,
                "today",  todayVisits,
                "month",  monthVisits,
                "total",  totalVisits
        );
    }

    private String getClientIdentifier(HttpServletRequest request) {
        String ip = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) userAgent = "unknown";
        return ip + "|" + (userAgent.length() > 50 ? userAgent.substring(0, 50) : userAgent);
    }

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
