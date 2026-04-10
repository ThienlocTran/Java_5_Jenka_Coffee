package com.springboot.jenka_coffee.api;

import com.springboot.jenka_coffee.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * FVULN-001 FIX: Server-side visitor counter — không thể fake bằng localStorage.
 * In-memory counter, reset today/month theo lịch.
 * Production scale: chuyển sang Redis hoặc DB.
 */
@RestController
@RequestMapping("/api/visitors")
public class ApiVisitorController {

    private final AtomicLong totalVisits  = new AtomicLong(0);
    private final AtomicLong todayVisits  = new AtomicLong(0);
    private final AtomicLong monthVisits  = new AtomicLong(0);
    private final AtomicLong onlineCount  = new AtomicLong(0);

    private LocalDate currentDay   = LocalDate.now();
    private int       currentMonth = LocalDate.now().getMonthValue();

    /**
     * POST /api/visitors/ping — gọi khi user load trang.
     * Tăng counter và trả về stats hiện tại.
     */
    @PostMapping("/ping")
    public ResponseEntity<ApiResponse<Map<String, Long>>> ping() {
        checkReset();
        totalVisits.incrementAndGet();
        todayVisits.incrementAndGet();
        monthVisits.incrementAndGet();
        // Online: đếm số ping trong 5 phút gần nhất (window-based)
        // Mỗi ping = 1 user đang active, reset mỗi 5 phút
        onlineCount.incrementAndGet();

        return ResponseEntity.ok(ApiResponse.success("OK", buildStats()));
    }

    /** GET /api/visitors/stats — lấy stats không tăng counter */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> stats() {
        return ResponseEntity.ok(ApiResponse.success("OK", buildStats()));
    }

    /** Giảm online count khi user rời trang (beacon API) */
    @PostMapping("/leave")
    public ResponseEntity<Void> leave() {
        if (onlineCount.get() > 0) onlineCount.decrementAndGet();
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

    /** Reset online count mỗi 5 phút (tránh drift) */
    @Scheduled(fixedDelay = 300_000)
    public void decayOnline() {
        // Online count = số ping trong 5 phút gần nhất
        // Reset về 0 mỗi 5 phút, để ping mới tích lũy lại
        // Thực tế hơn: đếm số request trong window 5 phút
        onlineCount.set(0);
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
        return Map.of(
                "online", Math.max(1, onlineCount.get()),
                "today",  todayVisits.get(),
                "month",  monthVisits.get(),
                "total",  totalVisits.get()
        );
    }
}
