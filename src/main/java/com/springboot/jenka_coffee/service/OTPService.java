package com.springboot.jenka_coffee.service;

/**
 * OTP service for phone number verification
 * Generates and validates 6-digit OTP codes
 */
public interface OTPService {

    /**
     * Generate OTP for phone number
     * 
     * @param phone Phone number
     * @return Generated OTP code
     */
    String generateOTP(String phone);

    /**
     * Verify OTP for phone number
     * 
     * @param phone Phone number
     * @param otp   OTP code to verify
     * @return true if valid, false otherwise
     */
    boolean verifyOTP(String phone, String otp);

    /**
     * Resend OTP to phone number
     * 
     * @param phone Phone number
     * @return New OTP code
     */
    String resendOTP(String phone);
}
