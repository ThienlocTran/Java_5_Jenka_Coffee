package com.springboot.jenka_coffee.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO cho POST /api/admin/products (tạo/cập nhật sản phẩm)
 * Gửi qua multipart/form-data (có imageFile upload)
 */
@Data
public class ProductRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(min = 2, max = 200, message = "Tên sản phẩm phải từ 2 đến 200 ký tự")
    private String name;

    @Size(max = 2000, message = "Mô tả không được vượt quá 2000 ký tự")
    private String description; // optional

    @NotNull(message = "Giá bán không được để trống")
    @DecimalMin(value = "1000", message = "Giá bán phải ít nhất 1,000 VNĐ")
    @DecimalMax(value = "10000000", message = "Giá bán không được vượt quá 10,000,000 VNĐ")
    private BigDecimal price;

    @NotBlank(message = "Vui lòng chọn danh mục sản phẩm")
    private String categoryId;

    private Boolean available = true;

    // imageFile được nhận qua @RequestParam MultipartFile – không bind vào DTO này
}
