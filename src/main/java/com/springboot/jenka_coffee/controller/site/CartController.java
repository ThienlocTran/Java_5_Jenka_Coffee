package com.springboot.jenka_coffee.controller.site;


import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

import com.springboot.jenka_coffee.service.CartService;

@Controller
@RequestMapping("/cart")
public class CartController {


    final CartService cartService;
    public CartController(CartService cartService) {
        this.cartService = cartService;
    }
    @GetMapping("/view")
    public String view(Model model) {
        model.addAttribute("items", cartService.getItems());
        model.addAttribute("total", cartService.getAmount());
        return "site/cart";
    }

    @GetMapping("/add/{id}")
    public String add(@PathVariable("id") Integer id) {
        cartService.add(id);
        return "redirect:/cart/view";
    }

    @GetMapping("/remove/{id}")
    public String remove(@PathVariable("id") Integer id) {
        cartService.remove(id);
        return "redirect:/cart/view";
    }

    @GetMapping("/update/{id}/{qty}")
    public String update(@PathVariable("id") Integer id, @PathVariable("qty") int qty) {
        cartService.update(id, qty);
        return "redirect:/cart/view";
    }

    @GetMapping("/clear")
    public String clear() {
        cartService.clear();
        return "redirect:/cart/view";
    }

    @GetMapping("/api/add/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addApi(@PathVariable("id") Integer id) {
        cartService.add(id);
        return ResponseEntity.ok(cartService.getCartSummary());
    }
}
