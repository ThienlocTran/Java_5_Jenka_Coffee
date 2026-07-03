package com.springboot.jenka_coffee.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * VULN-MASS-ASSIGNMENT FIX: DTO để nhận Product request
 * Chỉ cho phép các field được phép update, ngăn Mass Assignment
 */
@Data
public class ProductRequest {
    
    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;
    
    private String description;

    private String shortDescription;

    private String detailDescription;

    private String specificationsJson;

    private String featuresJson;

    private String warrantyInfo;

    private String shippingInfo;

    private String suitableFor;

    private String faqJson;

    private String metaTitle;

    private String metaDescription;
    
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá sản phẩm phải lớn hơn 0")
    private BigDecimal price;
    
    private Boolean available;
    
    private Boolean requireContact;

    private Boolean isHomeAddon;

    private Integer homeAddonPosition;
    
    // Image sẽ được xử lý riêng qua MultipartFile
    // Category ID sẽ được xử lý riêng qua @RequestParam
    
    // KHÔNG cho phép set: id, createDate, slug (auto-generated), quantity (không quản lý tồn kho)
}
