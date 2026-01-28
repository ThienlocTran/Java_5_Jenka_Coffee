package com.springboot.jenka_coffee.exception;

import lombok.Getter;

/**
 * Exception thrown when a business rule is violated
 */
@Getter
public class BusinessRuleException extends RuntimeException {

    private final String rule;

    public BusinessRuleException(String message) {
        super(message);
        this.rule = message;
    }

    public BusinessRuleException(String rule, String message) {
        super(message);
        this.rule = rule;
    }
}
