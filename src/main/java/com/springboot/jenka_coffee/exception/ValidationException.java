package com.springboot.jenka_coffee.exception;

import lombok.Getter;

/**
 * Custom exception for validation errors with field-specific messages
 */
@Getter
public class ValidationException extends RuntimeException {

    private final String field;
    private final String message;

    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
        this.message = message;
    }

    public ValidationException(String message) {
        this(null, message);
    }
}
