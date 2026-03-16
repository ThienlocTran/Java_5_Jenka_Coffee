package com.springboot.jenka_coffee.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO cho POST /api/admin/news (tạo/cập nhật tin tức)
 * Gửi qua multipart/form-data (có ImageFile upload)
 */
@Data
public class NewsRequest {

    @NotBlank(message = "Tiêu đề tin tức không được để trống")
    @Size(min = 5, max = 255, message = "Tiêu đề phải từ 5 đến 255 ký tự")
    private String title;

    @NotBlank(message = "Nội dung tin tức không được để trống")
    @Size(min = 20, message = "Nội dung phải từ 20 ký tự trở lên")
    private String content;

    private Boolean available = true;

    // imageFile được nhận qua @RequestParam MultipartFile – không bind vào DTO này
}
