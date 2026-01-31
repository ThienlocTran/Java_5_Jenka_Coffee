package com.springboot.jenka_coffee.dto.response;

/**
 * Stock status enum for product availability
 */
public enum StockStatus {
    IN_STOCK, // Quantity > 5
    LOW_STOCK, // Quantity 1-5
    OUT_OF_STOCK // Quantity = 0
}
