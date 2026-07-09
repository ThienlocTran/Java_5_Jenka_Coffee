package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.dto.response.CartItem;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

public interface CartService {
    void add(Integer productId);
    void remove(Integer productId);
    void update(Integer productId, int qty);
    void clear();
    Collection<CartItem> getItems();
    int getCount();
    BigDecimal getTotal();
    Map<String, Object> getCartSummary();
    void mergeAnonymousCart(String username, String anonymousCartId);
}
