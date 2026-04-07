package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.dto.request.ContactRequest;
import com.springboot.jenka_coffee.entity.Contact;
import com.springboot.jenka_coffee.repository.ContactRepository;
import com.springboot.jenka_coffee.service.ContactService;
import com.springboot.jenka_coffee.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ContactServiceImpl implements ContactService {

    private final ContactRepository contactRepository;
    private final EmailService emailService;

    public ContactServiceImpl(ContactRepository contactRepository, EmailService emailService) {
        this.contactRepository = contactRepository;
        this.emailService = emailService;
    }

    @Override
    public void sendContactEmail(ContactRequest request) {
        // Sanitize input trước khi lưu — ngăn Stored XSS trong admin panel
        Contact contact = new Contact();
        contact.setFullName(stripHtml(request.getFullName()));
        contact.setEmail(request.getEmail()); // email đã validate format
        contact.setSubject(stripHtml(request.getSubject()));
        contact.setMessage(stripHtml(request.getMessage()));
        contactRepository.save(contact);

        try {
            emailService.sendContactConfirmation(request.getEmail(), contact.getFullName(), contact.getSubject());
        } catch (Exception e) {
            log.warn("Không thể gửi email thông báo liên hệ");
        }
    }

    /** Strip HTML tags — ngăn Stored XSS */
    private String stripHtml(String input) {
        if (input == null) return null;
        return input.replaceAll("<[^>]*>", "").trim();
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
