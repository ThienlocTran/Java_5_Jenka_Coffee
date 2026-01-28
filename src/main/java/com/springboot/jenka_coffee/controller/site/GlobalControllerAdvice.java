package com.springboot.jenka_coffee.controller.site;

import com.springboot.jenka_coffee.dto.response.CartItem;
import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.service.CartService;
import com.springboot.jenka_coffee.service.CategoryService;
import com.springboot.jenka_coffee.util.ImageHelper;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final CategoryService categoryService;
    private final CartService cartService;
    private final ImageHelper imageHelper;

    public GlobalControllerAdvice(CategoryService categoryService,
            CartService cartService,
            ImageHelper imageHelper) {
        this.categoryService = categoryService;
        this.cartService = cartService;
        this.imageHelper = imageHelper;
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

    // Authentication attributes
    @ModelAttribute("currentUser")
    public Account getCurrentUser(HttpSession session) {
        return (Account) session.getAttribute("user");
    }

    @ModelAttribute("isAdmin")
    public boolean isAdmin(HttpSession session) {
        Account user = (Account) session.getAttribute("user");
        return user != null && user.getAdmin() != null && user.getAdmin();
    }

    @ModelAttribute("isLoggedIn")
    public boolean isLoggedIn(HttpSession session) {
        return session.getAttribute("user") != null;
    }

    // Image helper for fallback images
    @ModelAttribute("imageHelper")
    public ImageHelper getImageHelper() {
        return imageHelper;
    }
}
