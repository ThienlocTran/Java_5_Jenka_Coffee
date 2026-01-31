package com.springboot.jenka_coffee.service;

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
}
