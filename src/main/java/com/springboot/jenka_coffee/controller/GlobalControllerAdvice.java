package com.springboot.jenka_coffee.controller;

import com.springboot.jenka_coffee.dto.response.CartItem;
import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.service.CartService;
import com.springboot.jenka_coffee.service.CategoryService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final CategoryService categoryService;
    private final CartService cartService;

    public GlobalControllerAdvice(CategoryService categoryService,
       CartService cartService) {
        this.categoryService = categoryService;
        this.cartService = cartService;
    }

    @ModelAttribute("categories")
    public List<Category> populateCategories() {
        return categoryService.findAll();
    }

    @ModelAttribute("categoryIcons")
    public Map<String, String> populateCategoryIcons() {
        return categoryService.getCategoryIcons();
    }

    @ModelAttribute("cartCount")
    public int getCartCount() {
        return cartService.getCount();
    }

    @ModelAttribute("cartTotal")
    public double getCartTotal() {
        return cartService.getAmount();
    }

    @ModelAttribute("cartItems")
    public Collection<CartItem> getCartItems() {
        return cartService.getItems();
    }
}
