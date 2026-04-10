package com.springboot.jenka_coffee.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO cho PUT /api/profile/update
 * Hỗ trợ cập nhật thông tin cơ bản VÀ đổi mật khẩu trong cùng 1 request.
 * newPassword + confirmPassword chỉ cần khi muốn đổi mật khẩu.
 */
@Data
public class ProfileUpdateRequest {

    // fullname KHÔNG bắt buộc — cho phép update chỉ phone/email mà không cần gửi fullname
    @Size(min = 2, max = 100, message = "Họ và tên phải từ 2 đến 100 ký tự")
    private String fullname;

    @Email(message = "Email không đúng định dạng")
    @Size(max = 150, message = "Email không được vượt quá 150 ký tự")
    private String email;

    @Pattern(
        regexp = "^$|^(0|\\+84)(\\s|\\.)?" +
                 "((3[2-9])|(5[689])|(7[06-9])|(8[1-689])|(9[0-46-9]))" +
                 "(\\d)(\\s|\\.)?(\\d{3})(\\s|\\.)?(\\d{3})$",
        message = "Số điện thoại không hợp lệ (VD: 0901234567)"
    )
    private String phone;

    private String currentPassword;

    @Size(min = 6, max = 100, message = "Mật khẩu mới phải ít nhất 6 ký tự")
    private String newPassword;

    private String confirmPassword;
}