package com.springboot.jenka_coffee.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StoreFeedbackRequest {

    @NotBlank(message = "Chi nhánh không được để trống")
    @Pattern(regexp = "^(HN|HCM|ONLINE|OTHER)$", message = "Chi nhánh không hợp lệ")
    private String branch;

    @Size(min = 2, max = 100, message = "Họ và tên phải từ 2-100 ký tự")
    private String fullname;

    @Pattern(
            regexp = "^$|^(\\+84|84|0)(?:[\\s.-]?\\d){8,10}$",
            message = "Số điện thoại không hợp lệ"
    )
    private String phone;

    @JsonAlias({"comment"})
    @NotBlank(message = "Nội dung góp ý không được để trống")
    @Size(max = 2000, message = "Nội dung góp ý không được quá 2000 ký tự")
    private String content;

    @NotNull(message = "Đánh giá không được để trống")
    @Min(value = 1, message = "Đánh giá phải từ 1-5 sao")
    @Max(value = 5, message = "Đánh giá phải từ 1-5 sao")
    private Integer rating;
}
