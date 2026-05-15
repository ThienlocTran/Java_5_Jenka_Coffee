package com.springboot.jenka_coffee.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for revenue report data.
 * JPQL EXTRACT() returns Double, so constructors accept Number to handle both Double and null.
 */
@Data
@NoArgsConstructor
public class RevenueReportDTO {
    private Integer year;
    private Integer month; // null for yearly aggregation
    private BigDecimal totalRevenue;
    private Long orderCount;

    public RevenueReportDTO(Number year, Number month, BigDecimal totalRevenue, Long orderCount) {
        this.year = year != null ? year.intValue() : null;
        this.month = month != null ? month.intValue() : null;
        this.totalRevenue = totalRevenue;
        this.orderCount = orderCount;
    }
}
