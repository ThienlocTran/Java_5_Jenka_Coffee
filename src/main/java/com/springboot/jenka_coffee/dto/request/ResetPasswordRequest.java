package com.springboot.jenka_coffee.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO cho POST /api/auth/reset-password
 * Body JSON: { "token": "...", "newPassword": "...", "confirmPassword": "..." }
 */
@Data
public class ResetPasswordRequest {

    @NotBlank(message = "Token đặt lại mật khẩu không hợp lệ hoặc đã hết hạn")
    private String token;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 6, max = 100, message = "Mật khẩu phải ít nhất 6 ký tự")
    private String newPassword;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu")
    private String confirmPassword;
}
