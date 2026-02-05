package com.springboot.jenka_coffee.dto.request; // Nhớ package phải đúng đường dẫn

import lombok.Data;

@Data
public class ContactRequest {
    private String fullName;
    private String email;
    private String subject;
    private String message;
}