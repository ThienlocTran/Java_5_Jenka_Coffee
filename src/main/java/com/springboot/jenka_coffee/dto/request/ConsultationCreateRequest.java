package com.springboot.jenka_coffee.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConsultationCreateRequest {

    @Size(max = 100, message = "Tên không được vượt quá 100 ký tự")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập số điện thoại hoặc Zalo")
    @Size(max = 50, message = "Thông tin liên hệ không được vượt quá 50 ký tự")
    private String contactPhone;

    @NotBlank(message = "Vui lòng chọn nhu cầu sử dụng")
    private String needType;

    @NotBlank(message = "Vui lòng chọn hạng mục cần tư vấn")
    private String interest;

    @NotBlank(message = "Vui lòng chọn khoảng ngân sách")
    private String budget;

    @Size(max = 300, message = "Ghi chú tối đa 300 ký tự")
    private String note;

    @Size(max = 50, message = "Nguồn gửi không hợp lệ")
    private String source;

    @Size(max = 200, message = "Tên sản phẩm quá dài")
    private String productName;

    @Size(max = 500, message = "Liên kết sản phẩm quá dài")
    private String productUrl;

    @Size(max = 200, message = "Tiêu đề trang quá dài")
    private String pageTitle;

    @Size(max = 500, message = "Liên kết trang quá dài")
    private String pageUrl;
}
