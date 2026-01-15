package com.springboot.jenka_coffee.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AboutController {

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("brandName", "Jenka Coffee");
        model.addAttribute("slogan", "Awaken Your Daily Energy");
        return "site/menu/about-us";
    }
}