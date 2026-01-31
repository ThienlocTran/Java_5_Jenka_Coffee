package com.springboot.jenka_coffee.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminHomeController {

    /**
     * Admin home - redirect to dashboard
     */
    @GetMapping("")
    public String adminHome() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/")
    public String adminHomeSlash() {
        return "redirect:/admin/dashboard";
    }
}
