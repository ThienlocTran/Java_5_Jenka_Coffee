package com.springboot.jenka_coffee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for order history response
 * Includes order details with product information for display
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderHistoryDTO {
    /** Public-facing order code (e.g. "ORD-20260511-AB12CD"). Never expose numeric PK to frontend. */
    private String orderCode;
    private String address;
    private LocalDateTime createDate;
    private String phone;
    private Integer status;
    private BigDecimal totalAmount;
    private Integer pointsUsed;
    private String note;
    private List<OrderDetailDTO> orderDetails;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderDetailDTO {
        private Long id;
        private BigDecimal price;
        private Integer quantity;
        private ProductDTO product;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductDTO {
        private Integer id;
        private String name;
        private String image;
    }
}
