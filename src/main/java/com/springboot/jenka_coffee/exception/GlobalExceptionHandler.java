package com.springboot.jenka_coffee.exception;

import com.springboot.jenka_coffee.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.nio.channels.ClosedChannelException;

// BUG-64: EXCEPTION HANDLER SWALLOWS SECURITY EXCEPTIONS (Kẻ Nuốt Chửng Phân Quyền)
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

    // FIX: ResourceNotFoundException must return 404 Not Found, not 400 Bad Request
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        logger.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler({
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

    // ================================================================
    // SECURITY EXCEPTION HANDLERS (VULN #28 FIX)
    // ================================================================
    // These handlers MUST be defined BEFORE the catch-all Exception handler
    // to prevent security exceptions from being swallowed and returned as 500
    
    /**
     * Handle Spring Security AccessDeniedException (403 Forbidden)
     * Thrown when authenticated user lacks required role/permission
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException ex,
            HttpServletRequest request) {
        logger.warn("Access denied at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Bạn không có quyền truy cập tài nguyên này!"));
    }
    
    /**
     * Handle Spring Security AuthenticationException (401 Unauthorized)
     * Thrown when user is not authenticated (missing/invalid token)
     */
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationError(
            org.springframework.security.core.AuthenticationException ex,
            HttpServletRequest request) {
        logger.warn("Authentication failed at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Vui lòng đăng nhập để tiếp tục!"));
    }
    
    /**
     * Handle missing required headers (400 Bad Request)
     * Thrown when required header (e.g., Authorization) is missing
     */
    @ExceptionHandler(org.springframework.web.bind.MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingHeader(
            org.springframework.web.bind.MissingRequestHeaderException ex,
            HttpServletRequest request) {
        logger.warn("Missing required header at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Thiếu thông tin xác thực trong request!"));
    }
    
    // ================================================================
    // SPRING MVC EXCEPTION HANDLERS (TC-PRD-CTRL-040, TC-PRD-CTRL-041)
    // ================================================================
    
    /**
     * TC-PRD-CTRL-040 FIX: Handle missing required request parameters (400 Bad Request)
     * Thrown when required @RequestParam is missing from request
     * Example: POST /api/admin/products without categoryId parameter
     */
    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(
            org.springframework.web.bind.MissingServletRequestParameterException ex,
            HttpServletRequest request) {
        logger.warn("Missing required parameter at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Thiếu tham số bắt buộc: " + ex.getParameterName()));
    }
    
    /**
     * TC-PRD-CTRL-041 FIX: Handle unsupported media type (415 Unsupported Media Type)
     * Thrown when request Content-Type doesn't match expected type
     * Example: PUT /api/admin/products with Content-Type: application/json instead of multipart/form-data
     */
    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedMediaType(
            org.springframework.web.HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request) {
        logger.warn("Unsupported media type at {}: {}", request.getRequestURI(), ex.getMessage());
        String supportedTypes = ex.getSupportedMediaTypes().stream()
                .map(Object::toString)
                .reduce((a, b) -> a + ", " + b)
                .orElse("multipart/form-data");
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.error("Content-Type không hỗ trợ. Vui lòng sử dụng: " + supportedTypes));
    }
    
    // ================================================================
    // CATCH-ALL EXCEPTION HANDLER (MUST BE LAST)
    // ================================================================
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalError(Exception ex, HttpServletRequest request) {
        // Ignore client abort exceptions (user closed browser/tab before response completed)
        if (ex instanceof ClientAbortException ||
            ex instanceof AsyncRequestNotUsableException ||
            (ex.getCause() != null && ex.getCause() instanceof ClosedChannelException)) {
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
