package com.springboot.jenka_coffee.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConsultationStatusUpdateRequest {

    @NotBlank(message = "Vui lòng chọn trạng thái")
    private String status;
}
