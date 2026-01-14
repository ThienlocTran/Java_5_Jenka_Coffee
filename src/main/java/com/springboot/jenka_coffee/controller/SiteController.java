package com.springboot.jenka_coffee.controller;

import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class SiteController {

    private final ProductService productService;

    public SiteController(ProductService productService) {
        this.productService = productService;
    }

    // Auth mappings
    @GetMapping("/auth/login")
    public String login() {
        return "site/auth/login";
    }

    @GetMapping("/auth/signup")
    public String signup() {
        return "site/auth/sign-up";
    }

    @GetMapping("/auth/forgot-password")
    public String forgotPassword() {
        return "site/auth/forgot-password";
    }

    @GetMapping("/auth/change-password")
    public String changePassword() {
        return "site/auth/change-password";
    }

    @GetMapping("/auth/profile")
    public String profile() {
        return "site/auth/edit-profile";
    }

    @GetMapping("/auth/activate-success")
    public String activateSuccess() {
        return "site/auth/activate-success";
    }

    // Product mappings
    @GetMapping("/products/list")
    public String productList() {
        return "site/products/product-list";
    }

    @GetMapping("/product/detail/{id}")
    public String productDetail(@PathVariable("id") Integer id, Model model) {
        Product product = productService.findById(id);
        model.addAttribute("item", product);
        model.addAttribute("similarItems", productService.findByCategoryId(product.getCategory().getId()));
        return "site/products/product-detail";
    }

    // Cart & Checkout
    @GetMapping("/cart/view")
    public String cart() {
        return "site/cart";
    }

    @GetMapping("/checkout")
    public String checkout() {
        return "site/checkout";
    }

    // Orders
    @GetMapping("/orders/history")
    public String orderHistory() {
        return "site/order-list";
    }

    @GetMapping("/products/purchased")
    public String purchasedProducts() {
        return "site/my-products";
    }

    // Static pages
    @GetMapping("/about-us")
    public String aboutUs() {
        return "site/menu/about-us";
    }

    // Explicit index mapping (also covered by HomeController but good for
    // completeness locally)
    @GetMapping("/site/index")
    public String siteIndex() {
        return "site/index";
    }
}
