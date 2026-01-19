package com.springboot.jenka_coffee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartItem {
    private Integer productId;
    private String name;
    private String image;
    private Double price;
    private int quantity = 1;
}
