package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.dto.response.CartItem;
import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.service.CartService;
import com.springboot.jenka_coffee.service.ProductService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
@SessionScope
public class CartServiceImpl implements CartService {

    private final ObjectProvider<ProductService> productServiceProvider;

    private final Map<Integer, CartItem> map = new HashMap<>();

    public CartServiceImpl(ObjectProvider<ProductService> productServiceProvider) {
        this.productServiceProvider = productServiceProvider;
    }

    private ProductService productService() {
        return productServiceProvider.getObject();
    }

    @Override
    public void add(Integer productId) {
        CartItem item = map.get(productId);
        if (item == null) {
            Product product = productService().findById(productId);
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
        if (qty <= 0) {
            map.remove(productId); // qty <= 0 → xóa khỏi giỏ
            return;
        }
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
        // Trả về số loại sản phẩm (distinct), không phải tổng số lượng
        return map.size();
    }

    @Override
    public BigDecimal getTotal() {
        return map.values().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public Map<String, Object> getCartSummary() {
        Map<String, Object> response = new HashMap<>();
        response.put("count", getCount());
        response.put("total", getTotal());
        response.put("items", getItems());
        return response;
    }
}
