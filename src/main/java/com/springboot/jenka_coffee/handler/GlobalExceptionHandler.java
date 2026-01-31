package com.springboot.jenka_coffee.handler;

import com.springboot.jenka_coffee.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
     * Handle ResourceNotFoundException
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleResourceNotFound(ResourceNotFoundException ex,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {
        logger.error("Resource not found: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return getRedirectUrl(request);
    }

    /**
     * Handle ValidationException
     */
    @ExceptionHandler(ValidationException.class)
    public String handleValidation(ValidationException ex,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {
        logger.warn("Validation error: {} (field: {})", ex.getMessage(), ex.getField());
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        if (ex.getField() != null) {
            redirectAttributes.addFlashAttribute("fieldError", ex.getField());
        }
        return getRedirectUrl(request);
    }

    /**
     * Handle DuplicateResourceException
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public String handleDuplicateResource(DuplicateResourceException ex,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {
        logger.warn("Duplicate resource: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return getRedirectUrl(request);
    }

    /**
     * Handle BusinessRuleException
     */
    @ExceptionHandler(BusinessRuleException.class)
    public String handleBusinessRule(BusinessRuleException ex,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {
        logger.warn("Business rule violation: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return getRedirectUrl(request);
    }

    /**
     * Handle InvalidFileException
     */
    @ExceptionHandler(InvalidFileException.class)
    public String handleInvalidFile(InvalidFileException ex,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {
        logger.error("Invalid file: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return getRedirectUrl(request);
    }

    /**
     * Handle validation errors from @Valid annotation
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public String handleMethodArgumentNotValid(
            org.springframework.web.bind.MethodArgumentNotValidException ex,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        logger.warn("Validation error in request");

        // Extract first error message
        String errorMessage = ex.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Dữ liệu không hợp lệ");

        redirectAttributes.addFlashAttribute("error", errorMessage);
        return getRedirectUrl(request);
    }

    /**
     * Handle IllegalStateException (from business logic)
     */
    @ExceptionHandler(IllegalStateException.class)
    public String handleIllegalState(
            IllegalStateException ex,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        logger.warn("Illegal state: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return getRedirectUrl(request);
    }

    /**
     * Handle generic exceptions
     */
    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception ex,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        // Ignore favicon.ico errors (browser requests this automatically)
        if (ex instanceof NoResourceFoundException) {
            String requestUri = request.getRequestURI();
            if (requestUri != null && (requestUri.contains("favicon") || requestUri.contains("/product/"))) {
                return null; // Silently ignore - images are on cloud storage
            }
        }

        logger.error("Unexpected error: ", ex);
        redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + ex.getMessage());
        return getRedirectUrl(request);
    }

    /**
     * Get appropriate redirect URL based on request
     */
    private String getRedirectUrl(HttpServletRequest request) {
        String referer = request.getHeader("Referer");

        if (referer == null || referer.isEmpty()) {
            return "redirect:/admin/dashboard";
        }

        // If coming from edit/add form, redirect to list page
        if (referer.contains("/edit/") || referer.contains("/add")) {
            int lastSlash = referer.lastIndexOf("/");
            return "redirect:" + referer.substring(
                    referer.indexOf(request.getContextPath()) + request.getContextPath().length(), lastSlash);
        }

        // Otherwise redirect back to referer
        String path = referer.substring(referer.indexOf(request.getContextPath()) + request.getContextPath().length());
        return "redirect:" + path;
    }
}
