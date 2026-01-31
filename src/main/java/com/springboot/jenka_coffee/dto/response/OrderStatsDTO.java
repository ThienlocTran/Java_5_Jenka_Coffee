package com.springboot.jenka_coffee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for order statistics overview
 * Used in admin dashboard
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatsDTO {
    private Long totalOrders;
    private BigDecimal totalRevenue;
    private BigDecimal avgOrderValue;
}
