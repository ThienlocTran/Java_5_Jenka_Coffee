package com.springboot.jenka_coffee.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CheckoutRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(min = 3, max = 100, message = "Họ và tên phải từ 3-100 ký tự")
    @Pattern(regexp = "^[\\p{L}\\s]+$", message = "Họ và tên chỉ chứa chữ cái và khoảng trắng")
    private String fullname;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0|\\+84)(\\s|\\.)?((3[2-9])|(5[689])|(7[06-9])|(8[1-689])|(9[0-46-9]))(\\d)(\\s|\\.)?(\\d{3})(\\s|\\.)?(\\d{3})$", 
             message = "Số điện thoại không hợp lệ (VD: 0912345678)")
    private String phone;

    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(min = 10, max = 255, message = "Địa chỉ phải từ 10-255 ký tự")
    private String address;

    @NotBlank(message = "Vui lòng chọn Tỉnh/Thành phố")
    private String province;

    @NotBlank(message = "Vui lòng chọn Quận/Huyện")
    private String district;

    @NotBlank(message = "Vui lòng chọn Phường/Xã")
    private String ward;

    @NotBlank(message = "Vui lòng chọn phương thức thanh toán")
    private String paymentMethod;

    @Size(max = 500, message = "Ghi chú không được quá 500 ký tự")
    private String note;

    @AssertTrue(message = "Bạn phải đồng ý với điều khoản và điều kiện")
    private boolean agreeTerms;
}
