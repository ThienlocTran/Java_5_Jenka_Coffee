package com.springboot.jenka_coffee.controller.site;

import com.springboot.jenka_coffee.entity.ServiceBooking;
import com.springboot.jenka_coffee.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class BookingController {

    @Autowired
    private BookingService bookingService;

    // Hiển thị form đặt lịch
    @GetMapping("/booking")
    public String showBookingForm(Model model) {
        model.addAttribute("booking", new ServiceBooking());
        return "site/booking"; // Trỏ tới file booking.html trong templates/site
    }

    // Xử lý lưu form đặt lịch vào DB
    @PostMapping("/booking/submit")
    public String submitBooking(@ModelAttribute("booking") ServiceBooking booking) {
        booking.setStatus("PENDING"); // Mặc định là Pending (Chờ xác nhận)
        bookingService.save(booking);
        return "redirect:/booking?success"; // Quay lại trang form và hiện thông báo thành công
    }

}