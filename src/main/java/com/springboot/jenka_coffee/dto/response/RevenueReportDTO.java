package com.springboot.jenka_coffee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for revenue report data
 * Used for monthly/yearly analytics
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevenueReportDTO {
    private Integer year;
    private Integer month; // null for yearly aggregation
    private BigDecimal totalRevenue;
    private Long orderCount;
}
