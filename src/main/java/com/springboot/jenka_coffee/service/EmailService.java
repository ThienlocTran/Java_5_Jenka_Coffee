package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.entity.ConsultationRequest;

import java.math.BigDecimal;

/**
 * Email service for sending activation and password reset emails
 */
public interface EmailService {

    /**
     * Send activation email to new user
     * 
     * @param to       Recipient email address
     * @param token    Activation token
     * @param fullname User's full name
     */
    void sendActivationEmail(String to, String token, String fullname);

    /**
     * Send password reset email
     * 
     * @param to       Recipient email address
     * @param token    Reset token
     * @param fullname User's full name
     */
    void sendPasswordResetEmail(String to, String token, String fullname);

    /**
     * Notify admin when a new order is placed
     */
    void sendNewOrderNotification(String adminEmail, Long orderId, String customerName,
                                   String phone, String address, BigDecimal total);

    /**
     * Send a customer-facing order confirmation email after checkout succeeds.
     */
    void sendOrderConfirmation(String customerEmail, String customerName, Long orderId,
                               String orderCode, String phone, String address, BigDecimal total);

    void sendContactConfirmation(String toEmail, String customerName, String subject);

    void sendConsultationNotification(ConsultationRequest consultation);
}
