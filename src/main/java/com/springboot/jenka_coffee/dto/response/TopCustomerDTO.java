package com.springboot.jenka_coffee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for top customer rankings
 * Sorted by total spending
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopCustomerDTO {
    private String username;
    private String fullname;
    private BigDecimal totalSpent;
    private Long orderCount;
}
