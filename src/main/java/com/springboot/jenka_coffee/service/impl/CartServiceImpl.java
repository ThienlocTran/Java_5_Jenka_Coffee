package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.dto.response.CartItem;
import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.service.CartService;
import com.springboot.jenka_coffee.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
@SessionScope
public class CartServiceImpl implements CartService {

    @Autowired
    private ProductService productService;

    private Map<Integer, CartItem> map = new HashMap<>();

    @Override
    public void add(Integer productId) {
        CartItem item = map.get(productId);
        if (item == null) {
            Product product = productService.findById(productId);
            if (product != null) {
                item = new CartItem();
                item.setProductId(product.getId());
                item.setName(product.getName());
                item.setImage(product.getImage());
                item.setPrice(product.getPrice());
                item.setQuantity(1);
                map.put(productId, item);
            }
        } else {
            item.setQuantity(item.getQuantity() + 1);
        }
    }

    @Override
    public void remove(Integer productId) {
        map.remove(productId);
    }

    @Override
    public void update(Integer productId, int qty) {
        CartItem item = map.get(productId);
        if (item != null) {
            item.setQuantity(qty);
        }
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Collection<CartItem> getItems() {
        return map.values();
    }

    @Override
    public int getCount() {
        return map.values().stream()
                .mapToInt(item -> item.getQuantity())
                .sum();
    }

    @Override
    public double getAmount() {
        return map.values().stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();
    }
}
