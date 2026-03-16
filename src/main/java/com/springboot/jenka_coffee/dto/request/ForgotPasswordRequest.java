package com.springboot.jenka_coffee.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO cho POST /api/auth/forgot-password
 * Body JSON: { "identifier": "email_hoac_so_dien_thoai" }
 */
@Data
public class ForgotPasswordRequest {

    @NotBlank(message = "Vui lòng nhập email hoặc số điện thoại đã đăng ký")
    private String identifier;
}
