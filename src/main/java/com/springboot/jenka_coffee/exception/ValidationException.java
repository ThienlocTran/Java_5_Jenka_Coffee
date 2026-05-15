package com.springboot.jenka_coffee.exception;

import lombok.Getter;

/**
 * Custom exception for validation errors with field-specific messages
 */
@Getter
public class ValidationException extends RuntimeException {

    private final String field;
    private final String errorCode;

    public ValidationException(String field, String message) {
        this(field, message, "VALIDATION_ERROR");
    }

    public ValidationException(String field, String message, String errorCode) {
        super(message);
        this.field = field;
        this.errorCode = errorCode;
    }

    public ValidationException(String message) {
        this(null, message, "VALIDATION_ERROR");
    }
}
