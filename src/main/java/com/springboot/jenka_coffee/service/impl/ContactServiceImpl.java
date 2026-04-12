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
import org.springframework.transaction.annotation.Transactional;

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
        // VULN-OVER-SANITIZATION FIX: Use proper HTML escaping instead of stripping
        // Preserves user data while preventing XSS
        Contact contact = new Contact();
        contact.setFullName(sanitizeInput(request.getFullName()));
        contact.setEmail(request.getEmail()); // email đã validate format
        contact.setSubject(sanitizeInput(request.getSubject()));
        contact.setMessage(sanitizeInput(request.getMessage()));
        contactRepository.save(contact);

        try {
            emailService.sendContactConfirmation(request.getEmail(), contact.getFullName(), contact.getSubject());
        } catch (Exception e) {
            log.warn("Không thể gửi email thông báo liên hệ");
        }
    }

    /**
     * VULN-OVER-SANITIZATION FIX: Use OWASP HTML Sanitizer instead of regex stripping
     * Regex stripping destroys user data containing < or > characters
     * OWASP sanitizer preserves text while removing dangerous HTML
     */
    private static final org.owasp.html.PolicyFactory SANITIZE_POLICY =
            org.owasp.html.Sanitizers.FORMATTING.and(org.owasp.html.Sanitizers.LINKS);

    private String sanitizeInput(String input) {
        if (input == null) return null;
        // Sanitize HTML while preserving legitimate text content
        return SANITIZE_POLICY.sanitize(input).trim();
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
    @Transactional
    public void markAllAsRead() {
        contactRepository.markAllAsRead();
    }

    @Override
    public long countUnread() {
        return contactRepository.countByIsReadFalse();
    }
}
