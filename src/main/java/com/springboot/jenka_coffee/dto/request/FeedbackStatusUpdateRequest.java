package com.springboot.jenka_coffee.dto.request;

import com.springboot.jenka_coffee.entity.FeedbackStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FeedbackStatusUpdateRequest {

    @NotNull(message = "Trạng thái không được để trống")
    private FeedbackStatus status;
}
