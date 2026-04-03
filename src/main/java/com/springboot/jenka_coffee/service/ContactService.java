package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.dto.request.ContactRequest;
import com.springboot.jenka_coffee.entity.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ContactService {
    void sendContactEmail(ContactRequest request);
    Page<Contact> findAll(Pageable pageable);
    void markAsRead(Long id);
    long countUnread();
}
