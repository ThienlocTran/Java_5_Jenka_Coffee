package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.dto.request.ContactRequest;
import com.springboot.jenka_coffee.service.ContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class ContactServiceImpl implements ContactService {

    @Autowired
    private JavaMailSender mailSender;

    @Override
    public void sendContactEmail(ContactRequest request) throws Exception {
        // Logic tạo mail nằm gọn trong Service
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@jenkacoffee.com");
        message.setTo("admin@jenkacoffee.com");
        message.setSubject("Liên hệ mới từ: " + request.getFullName());
        message.setText("Họ tên: " + request.getFullName() + "\n"
                + "Email: " + request.getEmail() + "\n\n"
                + "Tiêu đề: " + request.getSubject() + "\n"
                + "Nội dung:\n" + request.getMessage());

        mailSender.send(message);
    }
}