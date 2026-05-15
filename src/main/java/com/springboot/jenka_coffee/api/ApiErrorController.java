package com.springboot.jenka_coffee.api;

import com.springboot.jenka_coffee.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller to handle all unmapped /error paths globally and return JSON instead of resolving an HTML view.
 */
@RestController
public class ApiErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<ApiResponse<Void>> handleError(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
        String message = (String) request.getAttribute("jakarta.servlet.error.message");
        
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (statusCode != null) {
            try {
                status = HttpStatus.valueOf(statusCode);
            } catch (Exception ex) {
                // Ignore, keep default 500 
            }
        }
        
        if (message == null || message.trim().isEmpty()) {
            message = status.getReasonPhrase();
        }
        
        if (status == HttpStatus.NOT_FOUND) {
            message = "Endpoint không tồn tại (404 Not Found)";
        }
        
        return ResponseEntity.status(status).body(ApiResponse.error(message));
    }
}
