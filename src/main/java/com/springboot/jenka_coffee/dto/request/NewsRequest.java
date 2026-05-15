package com.springboot.jenka_coffee.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * VULN-049 FIX: DTO cho News — không expose raw entity.
 * Ngăn mass assignment: id, createDate, available không được client set.
 */
@Data
public class NewsRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
    private String title;

    @Size(max = 50000, message = "Nội dung tối đa 50.000 ký tự")
    private String content;

    @Size(max = 500, message = "Tóm tắt tối đa 500 ký tự")
    private String summary;

    // available và createDate do server set — không nhận từ client
}
