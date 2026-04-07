package com.springboot.jenka_coffee.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO cho booking — KHÔNG expose entity trực tiếp.
 * Ngăn mass assignment attack (status, id override).
 */
@Data
public class BookingRequest {

    @NotBlank(message = "Vui lòng nhập họ tên")
    @Size(min = 2, max = 100, message = "Họ tên từ 2-100 ký tự")
    @Pattern(regexp = "^[\\p{L}\\s0-9]+$", message = "Họ tên không hợp lệ")
    private String customerName;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = "^(0|\\+84)(\\d{9,10})$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @Future(message = "Ngày hẹn phải là ngày trong tương lai")
    private LocalDate bookingDate;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    private String description;
}
