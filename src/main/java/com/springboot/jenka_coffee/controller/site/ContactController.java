package com.springboot.jenka_coffee.controller.site;

import com.springboot.jenka_coffee.dto.request.ContactRequest;
import com.springboot.jenka_coffee.service.ContactService; // Import Service
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class ContactController {

    @Autowired
    private ContactService contactService; // Tiêm Service vào

    @GetMapping("/contact")
    public String showContactForm(Model model) {
        model.addAttribute("contact", new ContactRequest());
        return "site/contact";
    }

    @PostMapping("/contact/send")
    public String sendContact(@ModelAttribute("contact") ContactRequest contact) {
        try {
            // Controller chỉ việc ra lệnh: "Service, gửi mail đi!"
            contactService.sendContactEmail(contact);
            return "redirect:/contact?success";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/contact?error";
        }
    }
}