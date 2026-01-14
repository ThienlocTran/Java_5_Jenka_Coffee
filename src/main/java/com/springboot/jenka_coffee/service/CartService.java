package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.dto.response.CartItem;
import java.util.Collection;

public interface CartService {
    void add(Integer productId);

    void remove(Integer productId);

    void update(Integer productId, int qty);

    void clear();

    Collection<CartItem> getItems();

    int getCount();

    double getAmount();
}
