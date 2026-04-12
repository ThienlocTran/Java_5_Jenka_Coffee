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

// 🚨 BUG-64: EXCEPTION HANDLER SWALLOWS SECURITY EXCEPTIONS (Kẻ Nuốt Chửng Phân Quyền)
// ================================================================
// CRITICAL UX/SECURITY ISSUE: Access denied (403) incorrectly returned as server error (500)!
// 
// Current state:
// - Spring Security throws AccessDeniedException when user lacks permission
// - Spring Security throws AuthenticationException when user not authenticated
// - GlobalExceptionHandler has catch-all @ExceptionHandler(Exception.class)
// - Catch-all handler returns 500 Internal Server Error for ALL exceptions
// - Security exceptions get swallowed and misreported
// 
// The problem:
// When user tries to access admin endpoint without permission:
// 1. User calls /api/admin/products (requires ROLE_ADMIN)
// 2. Spring Security checks roles → user only has ROLE_USER
// 3. Spring Security throws AccessDeniedException (should be 403 Forbidden)
// 4. GlobalExceptionHandler.handleGlobalError() catches it
// 5. Returns 500 Internal Server Error instead of 403 Forbidden
// 6. Frontend receives wrong status code
// 7. Frontend shows "Server error, try again later" instead of "Access denied"
// 
// User experience disaster:
// ```
// Expected flow:
// User clicks "Admin Panel" → 403 Forbidden → "You don't have permission" → Redirect to login
// 
// Actual flow:
// User clicks "Admin Panel" → 500 Server Error → "Server is down, try again later" → User confused
// ```
// 
// Frontend impact:
// ```javascript
// // Frontend Axios interceptor expects 403 for access denied
// axios.interceptors.response.use(
//   response => response,
//   error => {
//     if (error.response.status === 403) {
//       // Show "Access Denied" message
//       router.push('/access-denied');
//     } else if (error.response.status === 401) {
//       // Show "Please login" message
//       router.push('/login');
//     } else if (error.response.status === 500) {
//       // Show "Server error" message
//       showError('Server is having issues, please try again later');
//     }
//   }
// );
// 
// // But security exceptions return 500, so user sees wrong message!
// ```
// 
// Business impact:
// - Poor user experience (confusing error messages)
// - Users think server is broken when it's just permission issue
// - Support tickets increase ("Your website is down!")
// - Cannot distinguish real server errors from permission errors
// - Security issues masked as technical problems
// - Difficult to debug (logs show 500 for permission errors)
// 
// Root cause:
// Exception handler precedence in Spring:
// 1. Most specific exception handler runs first
// 2. If no specific handler, falls back to generic Exception.class handler
// 3. AccessDeniedException extends RuntimeException extends Exception
// 4. No specific handler for AccessDeniedException → caught by Exception.class
// 
// Current exception handlers:
// - ✓ ResourceNotFoundException → 400 Bad Request
// - ✓ ValidationException → 400 Bad Request
// - ✓ MaxUploadSizeExceededException → 400 Bad Request
// - ✗ AccessDeniedException → 500 (WRONG! Should be 403)
// - ✗ AuthenticationException → 500 (WRONG! Should be 401)
// - ✓ Exception (catch-all) → 500 Internal Server Error
// 
// Solution (NEEDS TO BE ADDED):
// 
// Add specific handlers for security exceptions BEFORE catch-all handler:
// ```java
// @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
// public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
//         org.springframework.security.access.AccessDeniedException ex) {
//     logger.warn("Access denied: {}", ex.getMessage());
//     return ResponseEntity.status(HttpStatus.FORBIDDEN)
//             .body(ApiResponse.error("Bạn không có quyền truy cập tài nguyên này!"));
// }
// 
// @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
// public ResponseEntity<ApiResponse<Void>> handleAuthenticationError(
//         org.springframework.security.core.AuthenticationException ex) {
//     logger.warn("Authentication failed: {}", ex.getMessage());
//     return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//             .body(ApiResponse.error("Vui lòng đăng nhập để tiếp tục!"));
// }
// 
// @ExceptionHandler(org.springframework.web.bind.MissingRequestHeaderException.class)
// public ResponseEntity<ApiResponse<Void>> handleMissingHeader(
//         org.springframework.web.bind.MissingRequestHeaderException ex) {
//     logger.warn("Missing required header: {}", ex.getMessage());
//     return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//             .body(ApiResponse.error("Thiếu thông tin xác thực trong request!"));
// }
// ```
// 
// HTTP status code best practices:
// - 200 OK: Success
// - 201 Created: Resource created successfully
// - 204 No Content: Success with no response body
// - 400 Bad Request: Invalid input data
// - 401 Unauthorized: Not authenticated (missing/invalid token)
// - 403 Forbidden: Authenticated but not authorized (insufficient permissions)
// - 404 Not Found: Resource doesn't exist
// - 409 Conflict: Duplicate resource (email already exists)
// - 422 Unprocessable Entity: Validation failed
// - 429 Too Many Requests: Rate limit exceeded
// - 500 Internal Server Error: Unexpected server error
// - 503 Service Unavailable: Server overloaded or maintenance
// 
// Security vs Authentication vs Authorization:
// ```
// 401 Unauthorized (Authentication failed):
// - No JWT token provided
// - JWT token expired
// - JWT token invalid signature
// - User not logged in
// Message: "Please login to continue"
// 
// 403 Forbidden (Authorization failed):
// - JWT token valid
// - User authenticated
// - User lacks required role/permission
// - User account locked/disabled
// Message: "You don't have permission to access this resource"
// 
// 500 Internal Server Error (Server fault):
// - Database connection failed
// - NullPointerException
// - OutOfMemoryError
// - Unexpected exception
// Message: "Server error, please try again later"
// ```
// 
// Testing:
// ```bash
// # Test 403 Forbidden (user tries to access admin endpoint)
// curl -H "Authorization: Bearer USER_TOKEN" http://localhost:8080/api/admin/products
// # Expected: 403 Forbidden
// # Actual (before fix): 500 Internal Server Error
// 
// # Test 401 Unauthorized (no token)
// curl http://localhost:8080/api/admin/products
// # Expected: 401 Unauthorized
// # Actual (before fix): 500 Internal Server Error
// 
// # Test 500 Internal Server Error (real error)
// curl http://localhost:8080/api/products/999999999
// # Expected: 404 Not Found (or 500 if database crashes)
// ```
// 
// Related issues:
// - CORS errors also return 500 (should be handled separately)
// - Rate limit errors return 500 (should be 429)
// - Validation errors correctly return 400 (already fixed)
// 
// Frontend error handling improvements:
// ```javascript
// // After fix, frontend can properly handle errors
// axios.interceptors.response.use(
//   response => response,
//   error => {
//     const status = error.response?.status;
//     const message = error.response?.data?.message;
//     
//     switch (status) {
//       case 400:
//         showError(message || 'Invalid input');
//         break;
//       case 401:
//         showError('Please login to continue');
//         router.push('/login');
//         break;
//       case 403:
//         showError('You do not have permission');
//         router.push('/access-denied');
//         break;
//       case 404:
//         showError('Resource not found');
//         break;
//       case 429:
//         showError('Too many requests, please slow down');
//         break;
//       case 500:
//         showError('Server error, please try again later');
//         break;
//       default:
//         showError('An error occurred');
//     }
//   }
// );
// ```
// 
// Monitoring:
// - Track 403 errors (permission issues - may indicate attack)
// - Track 401 errors (authentication issues - may indicate token theft)
// - Track 500 errors (real server errors - need investigation)
// - Alert on spike in 403/401 (possible brute force attack)
// ================================================================

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
