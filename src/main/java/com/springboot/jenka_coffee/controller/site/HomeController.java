package com.springboot.jenka_coffee.controller.site;

import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.service.CategoryService;
import com.springboot.jenka_coffee.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
    public String home(Model model,
            @RequestParam(value = "page", defaultValue = "0") int page) {
        // Pagination: 20 products per page for home
        Pageable pageable = PageRequest.of(page, 20);
        Page<Product> productPage = productService.findAllPaginated(pageable);

        model.addAttribute("items", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("categories", categoryService.findAll());

        // For promotions and related, use all products (not paginated)
        List<Product> allProducts = productService.findAll();
        List<Product> randomList = new ArrayList<>(allProducts);

        // Mocking promotions (3 items)
        Collections.shuffle(randomList);
        model.addAttribute("promotedItems", randomList.stream().limit(3).collect(Collectors.toList()));

        // Mocking related products (4 items)
        Collections.shuffle(randomList);
        model.addAttribute("relatedItems", randomList.stream().limit(4).collect(Collectors.toList()));

        return "index";
    }
}
