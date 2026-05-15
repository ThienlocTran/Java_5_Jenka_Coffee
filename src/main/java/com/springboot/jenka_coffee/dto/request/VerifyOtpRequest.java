package com.springboot.jenka_coffee.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * DTO cho POST /api/auth/verify-otp
 * Body JSON: { "phone": "...", "otp": "..." }
 */
@Data
public class VerifyOtpRequest {

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(
        regexp = "^(0|\\+84)(\\s|\\.)?" +
                 "((3[2-9])|(5[689])|(7[06-9])|(8[1-689])|(9[0-46-9]))" +
                 "(\\d)(\\s|\\.)?(\\d{3})(\\s|\\.)?(\\d{3})$",
        message = "Số điện thoại không hợp lệ"
    )
    private String phone;

    @NotBlank(message = "Mã OTP không được để trống")
    @Pattern(regexp = "^[0-9]{4,8}$", message = "Mã OTP phải là 4–8 chữ số")
    private String otp;
}
