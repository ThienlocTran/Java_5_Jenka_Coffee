package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.dto.request.ContactRequest;

public interface ContactService {
    void sendContactEmail(ContactRequest request) throws Exception;
}