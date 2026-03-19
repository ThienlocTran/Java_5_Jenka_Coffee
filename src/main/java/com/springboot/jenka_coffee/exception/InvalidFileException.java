package com.springboot.jenka_coffee.exception;

import lombok.Getter;

/**
 * Exception thrown when an uploaded file is invalid
 */
@Getter
public class InvalidFileException extends RuntimeException {

    private final String fileName;
    private final String reason;

    public InvalidFileException(String message) {
        super(message);
        this.fileName = null;
        this.reason = null;
    }
}
