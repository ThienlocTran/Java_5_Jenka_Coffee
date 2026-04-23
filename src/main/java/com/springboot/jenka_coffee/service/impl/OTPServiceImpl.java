package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.exception.ValidationException;
import com.springboot.jenka_coffee.service.OTPService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class OTPServiceImpl implements OTPService {

    private final Map<String, OTPData>     otpStore     = new ConcurrentHashMap<>();
    private final Map<String, AttemptData> attemptStore = new ConcurrentHashMap<>();

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS       = 5;

    @Override
    public String generateOTP(String phone) {
        String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        otpStore.put(phone, new OTPData(otp, LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES)));
        attemptStore.remove(phone); // reset attempts khi tạo OTP mới
        return otp;
    }

    @Override
    public boolean verifyOTP(String phone, String otp) {
        AttemptData attempts = attemptStore.computeIfAbsent(phone, k -> new AttemptData());
        if (attempts.count.get() >= MAX_ATTEMPTS) {
            otpStore.remove(phone);
            throw new ValidationException("Quá nhiều lần thử sai. OTP đã bị khóa. Vui lòng yêu cầu mã mới!");
        }

        OTPData stored = otpStore.get(phone);
        if (stored == null) return false;

        if (stored.expiry.isBefore(LocalDateTime.now())) {
            otpStore.remove(phone);
            attemptStore.remove(phone);
            return false;
        }

        // VULN-H03 FIX: Constant-time comparison — ngăn timing side-channel attack
        boolean match = java.security.MessageDigest.isEqual(
                stored.otp.getBytes(StandardCharsets.UTF_8),
                otp.getBytes(StandardCharsets.UTF_8));

        if (match) {
            otpStore.remove(phone);
            attemptStore.remove(phone);
            return true;
        }

        attempts.count.incrementAndGet();
        return false;
    }

    @Override
    public String resendOTP(String phone) {
        otpStore.remove(phone);
        attemptStore.remove(phone);
        return generateOTP(phone);
    }

    /** Dọn dẹp OTP và attempt data hết hạn mỗi 10 phút */
    @Scheduled(fixedDelay = 600_000)
    public void evictExpired() {
        LocalDateTime now = LocalDateTime.now();
        otpStore.entrySet().removeIf(e -> e.getValue().expiry.isBefore(now));
        // Xóa attempt data của các phone không còn OTP
        attemptStore.keySet().removeIf(phone -> !otpStore.containsKey(phone));
    }

    private static class OTPData {
        final String otp;
        final LocalDateTime expiry;
        OTPData(String otp, LocalDateTime expiry) { this.otp = otp; this.expiry = expiry; }
    }

    private static class AttemptData {
        final AtomicInteger count = new AtomicInteger(0);
    }
}
