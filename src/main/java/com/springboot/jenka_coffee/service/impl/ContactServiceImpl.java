package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.dto.request.ContactRequest;
import com.springboot.jenka_coffee.entity.Contact;
import com.springboot.jenka_coffee.repository.ContactRepository;
import com.springboot.jenka_coffee.service.ContactService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ContactServiceImpl implements ContactService {

    private final ContactRepository contactRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String adminEmail;

    public ContactServiceImpl(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    @Override
    public void sendContactEmail(ContactRequest request) {
        // Lưu vào DB trước — không phụ thuộc vào mail
        Contact contact = new Contact();
        contact.setFullName(request.getFullName());
        contact.setEmail(request.getEmail());
        contact.setSubject(request.getSubject());
        contact.setMessage(request.getMessage());
        contactRepository.save(contact);

        // Gửi mail thông báo cho admin — fail không ảnh hưởng
        if (mailSender != null && !adminEmail.isBlank()) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(adminEmail);
                message.setSubject("[Jenka Coffee] Liên hệ mới từ: " + request.getFullName());
                message.setText(
                    "Họ tên: " + request.getFullName() + "\n" +
                    "Email: " + request.getEmail() + "\n\n" +
                    "Tiêu đề: " + request.getSubject() + "\n" +
                    "Nội dung:\n" + request.getMessage()
                );
                mailSender.send(message);
            } catch (Exception e) {
                log.warn("Không thể gửi email thông báo liên hệ: {}", e.getMessage());
            }
        }
    }

    @Override
    public Page<Contact> findAll(Pageable pageable) {
        return contactRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Override
    public void markAsRead(Long id) {
        contactRepository.findById(id).ifPresent(c -> {
            c.setIsRead(true);
            contactRepository.save(c);
        });
    }

    @Override
    public long countUnread() {
        return contactRepository.countByIsReadFalse();
    }
}
