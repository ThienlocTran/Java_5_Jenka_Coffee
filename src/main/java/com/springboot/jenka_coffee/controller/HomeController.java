package com.springboot.jenka_coffee.controller;

import com.springboot.jenka_coffee.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HomeController {

    final ProductService productService;

    public HomeController(ProductService productService) {
        this.productService = productService;
    }

    @RequestMapping("/home")
    public String home(org.springframework.ui.Model model) {
        java.util.List<com.springboot.jenka_coffee.entity.Product> list = productService.findAll();
        model.addAttribute("items", list);

        // Mocking related products by taking 4 random items (or first 4)
        // In a real app, you might query by category or tags
        java.util.Collections.shuffle(list);
        model.addAttribute("relatedItems", list.stream().limit(4).collect(java.util.stream.Collectors.toList()));

        return "index";
    }
}
