package com.springboot.jenka_coffee.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleLoginRequest {
    @NotBlank(message = "Google ID token is required")
    private String idToken;
    
    private String phone; // Optional - for phone number update after login
}
