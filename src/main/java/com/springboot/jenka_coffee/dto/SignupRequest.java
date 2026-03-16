package com.springboot.jenka_coffee.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SignupRequest {

    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 3, max = 50, message = "Tên đăng nhập phải từ 3 đến 50 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Tên đăng nhập chỉ được chứa chữ cái, số và dấu gạch dưới")
    private String username;

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(min = 3, max = 100, message = "Họ và tên phải từ 3 đến 100 ký tự")
    @Pattern(regexp = "^[\\p{L}\\s]+$", message = "Họ và tên chỉ được chứa chữ cái và khoảng trắng")
    private String fullname;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(
        regexp = "^(0|\\+84)(\\s|\\.)?" +
                 "((3[2-9])|(5[689])|(7[06-9])|(8[1-689])|(9[0-46-9]))" +
                 "(\\d)(\\s|\\.)?(\\d{3})(\\s|\\.)?(\\d{3})$",
        message = "Số điện thoại không hợp lệ (VD: 0901234567)"
    )
    private String phone;

    @Email(message = "Email không đúng định dạng")
    private String email; // optional

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, max = 100, message = "Mật khẩu phải ít nhất 6 ký tự")
    private String password;
}
