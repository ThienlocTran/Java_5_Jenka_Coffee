package com.springboot.jenka_coffee.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global exception handler for all controllers
 * Centralizes error handling and provides consistent error messages
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle business logic exceptions - Redirect with flash message
     */
    @ExceptionHandler({
            ResourceNotFoundException.class,
            ValidationException.class,
            DuplicateResourceException.class,
            BusinessRuleException.class,
            InvalidFileException.class,
            IllegalStateException.class
    })
    public String handleBusinessExceptions(RuntimeException ex, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        logger.warn("Business exception: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        if (ex instanceof ValidationException && ((ValidationException) ex).getField() != null) {
            redirectAttributes.addFlashAttribute("fieldError", ((ValidationException) ex).getField());
        }
        return getRedirectUrl(request);
    }

    /**
     * Handle validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public String handleValidationErrors(MethodArgumentNotValidException ex, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        String errorMessage = ex.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Dữ liệu không hợp lệ");
        
        redirectAttributes.addFlashAttribute("error", errorMessage);
        return getRedirectUrl(request);
    }

    /**
     * Handle 404 - Page Not Found
     */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(Exception ex, Model model) {
        logger.warn("Page not found: {}", ex.getMessage());
        return "error/404";
    }

    /**
     * Handle 500 - Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGlobalError(Exception ex, Model model) {
        // Ignore favicon requests
        if (ex.getMessage() != null && ex.getMessage().contains("favicon")) {
            return null;
        }
        
        logger.error("Internal Server Error", ex);
        model.addAttribute("errorMessage", "Đã có lỗi xảy ra. Vui lòng thử lại sau!");
        model.addAttribute("debugMessage", ex.getMessage());
        return "error/500";
    }

    private String getRedirectUrl(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isEmpty()) {
            return "redirect:/home";
        }
        // Avoid redirect loop if referer is same as error URL (simplified)
        return "redirect:" + referer;
    }
}
