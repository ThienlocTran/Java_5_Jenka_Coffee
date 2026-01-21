package com.springboot.jenka_coffee.controller;

import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.service.CategoryService;
import com.springboot.jenka_coffee.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    final ProductService productService;
    final CategoryService categoryService;

    public HomeController(ProductService productService,
            CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @RequestMapping("/home")
    public String home(Model model) {
        List<Product> list = productService.findAll();
        model.addAttribute("items", list);
        model.addAttribute("categories", categoryService.findAll());

        // Create a copy for random selection to preserve original list order if needed
        // (though here we just passed list directly above)
        // If we want 'items' to be stable, we should pass 'list' before shuffling, or
        // copy it.
        // Assuming 'list' is mutable.
        List<Product> randomList = new ArrayList<>(list);

        // Mocking promotions (3 items)
        Collections.shuffle(randomList);
        model.addAttribute("promotedItems", randomList.stream().limit(3).collect(Collectors.toList()));

        // Mocking related products (4 items)
        Collections.shuffle(randomList);
        model.addAttribute("relatedItems", randomList.stream().limit(4).collect(Collectors.toList()));

        return "index";
    }
}
