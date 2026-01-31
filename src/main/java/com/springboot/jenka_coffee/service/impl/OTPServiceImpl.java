package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.service.OTPService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory OTP service implementation
 * For production, consider using Redis for distributed caching
 */
@Service
public class OTPServiceImpl implements OTPService {

    private final Map<String, OTPData> otpStore = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;

    @Override
    public String generateOTP(String phone) {
        // Generate 6-digit OTP
        String otp = String.format("%06d", random.nextInt(1000000));

        // Store with expiry
        OTPData otpData = new OTPData(otp, LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        otpStore.put(phone, otpData);

        // TODO: Integrate SMS provider (Twilio, AWS SNS, etc.)
        // For now, just log it
        System.out.println("=== OTP GENERATED ===");
        System.out.println("Phone: " + phone);
        System.out.println("OTP: " + otp);
        System.out.println("Expires: " + otpData.expiry);
        System.out.println("====================");

        return otp;
    }

    @Override
    public boolean verifyOTP(String phone, String otp) {
        OTPData storedData = otpStore.get(phone);

        if (storedData == null) {
            return false; // No OTP found
        }

        if (storedData.expiry.isBefore(LocalDateTime.now())) {
            otpStore.remove(phone); // Remove expired OTP
            return false;
        }

        if (storedData.otp.equals(otp)) {
            otpStore.remove(phone); // Remove used OTP
            return true;
        }

        return false;
    }

    @Override
    public String resendOTP(String phone) {
        // Remove old OTP and generate new one
        otpStore.remove(phone);
        return generateOTP(phone);
    }

    /**
     * Inner class to store OTP with expiry time
     */
    private static class OTPData {
        String otp;
        LocalDateTime expiry;

        OTPData(String otp, LocalDateTime expiry) {
            this.otp = otp;
            this.expiry = expiry;
        }
    }
}
