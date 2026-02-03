package com.springboot.jenka_coffee.dto.request;

import lombok.Data;

@Data
public class ProfileUpdateRequest {
    private String fullname;
    private String email;
    private String phone;
    private String currentPassword; // Để xác thực khi thay đổi thông tin nhạy cảm
    private String newPassword;     // Nếu muốn đổi mật khẩu
    private String confirmPassword; // Xác nhận mật khẩu mới
}