package com.springboot.jenka_coffee.entity;

public enum ProductKind {
    COFFEE_MACHINE,
    GRINDER,
    ACCESSORY,
    OTHER;

    public static ProductKind fromNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ProductKind.valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid productKind: " + value);
        }
    }
}
