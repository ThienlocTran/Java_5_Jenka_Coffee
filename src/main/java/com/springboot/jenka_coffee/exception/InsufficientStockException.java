package com.springboot.jenka_coffee.exception;

import lombok.Getter;

/**
 * Exception thrown when attempting to checkout with insufficient product stock
 */
@Getter
public class InsufficientStockException extends RuntimeException {

    private final String productName;
    private final Integer requestedQuantity;
    private final Integer availableQuantity;

    public InsufficientStockException(String productName, Integer requestedQuantity, Integer availableQuantity) {
        super(String.format("Sản phẩm '%s' chỉ còn %d trong kho, không đủ cho số lượng yêu cầu: %d",
                productName, availableQuantity, requestedQuantity));
        this.productName = productName;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
    }

}
