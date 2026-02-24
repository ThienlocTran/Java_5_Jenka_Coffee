package com.springboot.jenka_coffee.controller.admin;

import com.springboot.jenka_coffee.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/booking")
public class AdminBookingController {

    @Autowired
    private BookingService bookingService;

    @GetMapping("/list")
    public String list(Model model, @RequestParam(defaultValue = "0") int page) {
        Pageable pageable = PageRequest.of(page, 10);
        model.addAttribute("bookings", bookingService.findAll(pageable));
        return "admin/booking/list";
    }

    @PostMapping("/update-status")
    public String updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        bookingService.updateStatus(id, status);
        return "redirect:/admin/booking/list";
    }
}