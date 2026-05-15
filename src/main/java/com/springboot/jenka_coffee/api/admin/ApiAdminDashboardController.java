package com.springboot.jenka_coffee.api.admin;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.dto.response.DashboardCountsDTO;
import com.springboot.jenka_coffee.dto.response.OrderStatsDTO;
import com.springboot.jenka_coffee.dto.response.RevenueReportDTO;
import com.springboot.jenka_coffee.dto.response.TopProductDTO;
import com.springboot.jenka_coffee.entity.Order;
import com.springboot.jenka_coffee.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/dashboard")
public class ApiAdminDashboardController {

    private final ReportService reportService;

    public ApiAdminDashboardController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        OrderStatsDTO stats = reportService.getOrderStats();
        DashboardCountsDTO counts = reportService.getDashboardCounts();

        int currentYear = LocalDateTime.now().getYear();
        List<RevenueReportDTO> monthlyRevenue = reportService.getMonthlyRevenue(currentYear);
        
        // Fill missing months with 0 revenue
        Map<Integer, BigDecimal> revenueByMonth = monthlyRevenue.stream()
                .collect(java.util.stream.Collectors.toMap(
                        r -> r.getMonth() != null ? r.getMonth() : 0,
                        r -> r.getTotalRevenue() != null ? r.getTotalRevenue() : BigDecimal.ZERO
                ));
        
        List<Map<String, Object>> chartData = new java.util.ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("month", m);
            entry.put("revenue", revenueByMonth.getOrDefault(m, BigDecimal.ZERO));
            chartData.add(entry);
        }

        List<Order> recentOrders = reportService.getRecentOrders(5);
        List<Map<String, Object>> orderDTOs = recentOrders.stream()
                .sorted(java.util.Comparator.comparing(Order::getCreateDate, 
                        java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .map(o -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", o.getId());
            dto.put("createDate", o.getCreateDate() != null ? o.getCreateDate().toString() : null);
            dto.put("totalAmount", o.getTotalAmount());
            dto.put("status", o.getStatus());
            if (o.getAccount() != null) {
                dto.put("account", Map.of(
                    "fullname", o.getAccount().getFullname(),
                    "phone",    o.getAccount().getPhone() != null ? o.getAccount().getPhone() : ""
                ));
            } else {
                dto.put("account", null);
            }
            return dto;
        }).collect(Collectors.toList());

        List<TopProductDTO> topProducts = reportService.getTopProducts(5);
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        LocalDateTime currentMonthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime nextMonthStart = currentMonthStart.plusMonths(1);
        LocalDateTime previousMonthStart = currentMonthStart.minusMonths(1);
        BigDecimal currentMonthRevenue = reportService.getTotalRevenueBetween(currentMonthStart, nextMonthStart);
        BigDecimal previousMonthRevenue = reportService.getTotalRevenueBetween(previousMonthStart, currentMonthStart);

        Map<String, Object> data = new HashMap<>();
        data.put("totalRevenue",   stats.getTotalRevenue() != null ? stats.getTotalRevenue() : BigDecimal.ZERO);
        data.put("revenueGrowth",  calculateGrowthPercent(currentMonthRevenue, previousMonthRevenue));
        data.put("totalOrders",    counts.totalOrders());
        data.put("newOrdersToday", reportService.countOrdersBetween(todayStart, tomorrowStart));
        data.put("totalProducts",  counts.totalProducts());
        data.put("newProducts",    reportService.countProductsBetween(currentMonthStart, nextMonthStart));
        data.put("totalCustomers", counts.totalCustomers());
        data.put("newCustomers",   reportService.countCustomersBetween(currentMonthStart, nextMonthStart));
        data.put("recentOrders",   orderDTOs);
        data.put("monthlyRevenue", chartData);
        data.put("topProducts",    topProducts);

        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin dashboard thành công", data));
    }

    private BigDecimal calculateGrowthPercent(BigDecimal current, BigDecimal previous) {
        BigDecimal safeCurrent = current != null ? current : BigDecimal.ZERO;
        BigDecimal safePrevious = previous != null ? previous : BigDecimal.ZERO;

        if (safePrevious.compareTo(BigDecimal.ZERO) == 0) {
            return safeCurrent.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }

        return safeCurrent.subtract(safePrevious)
                .multiply(BigDecimal.valueOf(100))
                .divide(safePrevious, 1, RoundingMode.HALF_UP);
    }

    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRevenue(
            @RequestParam(defaultValue = "month") String period,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer value) {

        int y = year != null ? year : LocalDateTime.now().getYear();
        LocalDateTime from, toExclusive;

        switch (period) {
            case "week" -> {
                int week = value != null ? value : LocalDateTime.now().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                from = java.time.LocalDate.of(y, 1, 1)
                        .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, (long) week)
                        .with(DayOfWeek.MONDAY)
                        .atStartOfDay();
                toExclusive = from.plusDays(7);
            }
            case "quarter" -> {
                int q = value != null ? value : (LocalDateTime.now().getMonthValue() - 1) / 3 + 1;
                int startMonth = (q - 1) * 3 + 1;
                from = LocalDateTime.of(y, startMonth, 1, 0, 0);
                toExclusive = from.plusMonths(3);
            }
            case "year" -> {
                from = LocalDateTime.of(y, 1, 1, 0, 0);
                toExclusive = from.plusYears(1);
            }
            default -> { // month
                int m = value != null ? value : LocalDateTime.now().getMonthValue();
                from = LocalDateTime.of(y, m, 1, 0, 0);
                toExclusive = from.plusMonths(1);
            }
        }

        List<Order> orders = reportService.getOrdersForRevenueReport(from, toExclusive);
        List<Map<String, Object>> revenueData = buildRevenueTimeline(period, from, toExclusive, orders);
        List<TopProductDTO> topProducts = reportService.getTopProducts(10);
        BigDecimal totalRevenue = revenueData.stream()
                .map(row -> (BigDecimal) row.get("totalRevenue"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalOrders = revenueData.stream()
                .mapToLong(row -> ((Number) row.get("orderCount")).longValue())
                .sum();
        BigDecimal avgOrderValue = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        Map<String, Object> bestPeriod = revenueData.stream()
                .max((a, b) -> ((BigDecimal) a.get("totalRevenue")).compareTo((BigDecimal) b.get("totalRevenue")))
                .orElse(Map.of());
        LocalDateTime previousFrom = getPreviousPeriodStart(period, from);
        BigDecimal previousPeriodRevenue = reportService.getTotalRevenueBetween(previousFrom, from);
        BigDecimal growthPercent = calculateGrowthPercent(totalRevenue, previousPeriodRevenue);

        Map<String, Object> result = new HashMap<>();
        result.put("data", revenueData);
        result.put("topProducts", topProducts);
        result.put("from", from.toString());
        result.put("to", toExclusive.minusSeconds(1).toString());
        result.put("summary", Map.of(
                "totalRevenue", totalRevenue,
                "totalOrders", totalOrders,
                "averageOrderValue", avgOrderValue,
                "bestPeriod", bestPeriod,
                "previousPeriodRevenue", previousPeriodRevenue,
                "growthPercent", growthPercent
        ));

        return ResponseEntity.ok(ApiResponse.success("OK", result));
    }

    private LocalDateTime getPreviousPeriodStart(String period, LocalDateTime currentStart) {
        return switch (period) {
            case "week" -> currentStart.minusWeeks(1);
            case "quarter" -> currentStart.minusMonths(3);
            case "year" -> currentStart.minusYears(1);
            default -> currentStart.minusMonths(1);
        };
    }

    private List<Map<String, Object>> buildRevenueTimeline(
            String period,
            LocalDateTime from,
            LocalDateTime toExclusive,
            List<Order> orders) {

        Map<String, Map<String, Object>> buckets = new LinkedHashMap<>();

        if ("quarter".equals(period) || "year".equals(period)) {
            LocalDate cursor = from.toLocalDate().withDayOfMonth(1);
            while (cursor.isBefore(toExclusive.toLocalDate())) {
                String key = cursor.getYear() + "-" + String.format("%02d", cursor.getMonthValue());
                buckets.put(key, createRevenueBucket(
                        key,
                        "T" + cursor.getMonthValue() + "/" + cursor.getYear(),
                        cursor.getYear(),
                        cursor.getMonthValue(),
                        null));
                cursor = cursor.plusMonths(1);
            }
        } else {
            DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("dd/MM");
            LocalDate cursor = from.toLocalDate();
            while (cursor.isBefore(toExclusive.toLocalDate())) {
                String key = cursor.toString();
                buckets.put(key, createRevenueBucket(
                        key,
                        labelFormatter.format(cursor),
                        cursor.getYear(),
                        cursor.getMonthValue(),
                        cursor.getDayOfMonth()));
                cursor = cursor.plusDays(1);
            }
        }

        for (Order order : orders) {
            if (order.getCreateDate() == null) {
                continue;
            }
            LocalDate orderDate = order.getCreateDate().toLocalDate();
            String key = ("quarter".equals(period) || "year".equals(period))
                    ? orderDate.getYear() + "-" + String.format("%02d", orderDate.getMonthValue())
                    : orderDate.toString();
            Map<String, Object> bucket = buckets.get(key);
            if (bucket == null) {
                continue;
            }

            BigDecimal currentRevenue = (BigDecimal) bucket.get("totalRevenue");
            BigDecimal orderAmount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
            long orderCount = ((Number) bucket.get("orderCount")).longValue() + 1;
            BigDecimal newRevenue = currentRevenue.add(orderAmount);

            bucket.put("totalRevenue", newRevenue);
            bucket.put("orderCount", orderCount);
            bucket.put("averageOrderValue", orderCount > 0
                    ? newRevenue.divide(BigDecimal.valueOf(orderCount), 0, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
        }

        return new ArrayList<>(buckets.values());
    }

    private Map<String, Object> createRevenueBucket(String periodKey, String label, int year, int month, Integer day) {
        Map<String, Object> bucket = new LinkedHashMap<>();
        bucket.put("period", periodKey);
        bucket.put("label", label);
        bucket.put("year", year);
        bucket.put("month", month);
        bucket.put("day", day);
        bucket.put("totalRevenue", BigDecimal.ZERO);
        bucket.put("orderCount", 0L);
        bucket.put("averageOrderValue", BigDecimal.ZERO);
        return bucket;
    }

    private BigDecimal calculateTimelineGrowth(List<Map<String, Object>> rows) {
        Map<String, Object> latest = null;
        Map<String, Object> previous = null;

        for (int i = rows.size() - 1; i >= 0; i--) {
            BigDecimal revenue = (BigDecimal) rows.get(i).get("totalRevenue");
            if (revenue.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (latest == null) {
                latest = rows.get(i);
            } else {
                previous = rows.get(i);
                break;
            }
        }

        if (latest == null || previous == null) {
            return BigDecimal.ZERO;
        }

        return calculateGrowthPercent(
                (BigDecimal) latest.get("totalRevenue"),
                (BigDecimal) previous.get("totalRevenue"));
    }
}

