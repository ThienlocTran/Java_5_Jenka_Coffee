package com.springboot.jenka_coffee.exception;

import com.springboot.jenka_coffee.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global exception handler — returns JSON for all controllers.
 * Replaces the old Thymeleaf-based ControllerAdvice.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientStock(InsufficientStockException ex) {
        logger.warn("Insufficient stock: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler({
            ResourceNotFoundException.class,
            ValidationException.class,
            DuplicateResourceException.class,
            BusinessRuleException.class,
            InvalidFileException.class,
            IllegalStateException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBusinessExceptions(RuntimeException ex) {
        logger.warn("Business exception: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxSizeException(MaxUploadSizeExceededException ex) {
        logger.warn("Upload size exceeded: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Kích thước tệp tin tải lên quá lớn! Vui lòng chọn tệp nhỏ hơn 10MB."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        String friendly;
        if (msg.contains("phone") || msg.contains("accounts_phone_key")) {
            friendly = "Số điện thoại này đã được đăng ký. Vui lòng dùng số khác.";
        } else if (msg.contains("email") || msg.contains("accounts_email_key")) {
            friendly = "Email này đã được đăng ký. Vui lòng dùng email khác.";
        } else if (msg.contains("username") || msg.contains("accounts_pkey")) {
            friendly = "Tên đăng nhập đã tồn tại. Vui lòng chọn tên khác.";
        } else {
            friendly = "Dữ liệu đã tồn tại. Vui lòng kiểm tra lại thông tin.";
        }
        logger.warn("Data integrity violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(friendly));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, org.springframework.validation.BindException.class})
    public ResponseEntity<ApiResponse<Void>> handleValidationErrors(Exception ex) {
        org.springframework.validation.BindingResult bindingResult = null;
        if (ex instanceof MethodArgumentNotValidException e) {
            bindingResult = e.getBindingResult();
        } else if (ex instanceof org.springframework.validation.BindException e) {
            bindingResult = e.getBindingResult();
        }
        String message = bindingResult != null
                ? bindingResult.getAllErrors().stream()
                        .findFirst()
                        .map(DefaultMessageSourceResolvable::getDefaultMessage)
                        .orElse("Dữ liệu đầu vào không hợp lệ")
                : "Dữ liệu đầu vào không hợp lệ";
        logger.warn("Validation error: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    // Static resource paths that should silently return 404 (no logging, no object allocation)
    private static final String[] SILENT_404_PREFIXES = {
            "/images/", "/uploads/", "/css/", "/js/", "/static/", "/public/", "/favicon"
    };
    private static final String[] SILENT_404_EXTENSIONS = {
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg", ".ico",
            ".css", ".js", ".woff", ".woff2", ".ttf", ".eot", ".map"
    };

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<Void> handleNotFound(Exception ex, HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri != null) {
            String lower = uri.toLowerCase();
            for (String prefix : SILENT_404_PREFIXES) {
                if (lower.startsWith(prefix)) {
                    return ResponseEntity.notFound().build();
                }
            }
            for (String ext : SILENT_404_EXTENSIONS) {
                if (lower.endsWith(ext)) {
                    return ResponseEntity.notFound().build();
                }
            }
        }
        logger.warn("Not found: {}", ex.getMessage());
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalError(Exception ex, HttpServletRequest request) {
        // Ignore client abort exceptions (user closed browser/tab before response completed)
        if (ex instanceof org.apache.catalina.connector.ClientAbortException ||
            ex instanceof org.springframework.web.context.request.async.AsyncRequestNotUsableException ||
            (ex.getCause() != null && ex.getCause() instanceof java.nio.channels.ClosedChannelException)) {
            // Silent ignore - this is normal when user navigates away
            return null;
        }
        
        // Ignore favicon noise
        String uri = request.getRequestURI();
        if (uri != null && uri.contains("favicon")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        
        // SECURITY FIX: Không leak stack trace ra client
        // Log chi tiết ở backend, trả generic message cho client
        logger.error("Internal Server Error at {}: {}", uri, ex.getMessage(), ex);
        
        // Production: Generic error message
        // Development: Có thể thêm chi tiết (check via profile)
        String errorMessage = "Đã có lỗi xảy ra. Vui lòng thử lại sau!";
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(errorMessage));
    }
}
