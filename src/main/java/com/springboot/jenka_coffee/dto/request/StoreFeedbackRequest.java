package com.springboot.jenka_coffee.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class StoreFeedbackRequest {

    @NotBlank(message = "Chi nhánh không được để trống")
    @Pattern(regexp = "^(HN|HCM)$", message = "Chi nhánh chỉ được là HN hoặc HCM")
    private String branch;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(min = 2, max = 100, message = "Họ tên phải từ 2-100 ký tự")
    private String fullname;

    @Pattern(regexp = "^(0|\\+84)\\d{9}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @Size(max = 500, message = "Nhận xét không được quá 500 ký tự")
    private String comment;

    @NotNull(message = "Đánh giá cửa hàng không được để trống")
    @Min(value = 1, message = "Đánh giá cửa hàng phải từ 1-5 sao")
    @Max(value = 5, message = "Đánh giá cửa hàng phải từ 1-5 sao")
    private Integer storeRating;

    @NotNull(message = "Đánh giá nhân viên không được để trống")
    @Min(value = 1, message = "Đánh giá nhân viên phải từ 1-5 sao")
    @Max(value = 5, message = "Đánh giá nhân viên phải từ 1-5 sao")
    private Integer staffRating;
}
