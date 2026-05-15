package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.exception.ValidationException;
import com.springboot.jenka_coffee.service.OTPService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class OTPServiceImpl implements OTPService {

    private final Map<String, OTPData>     otpStore     = new ConcurrentHashMap<>();
    private final Map<String, AttemptData> attemptStore = new ConcurrentHashMap<>();
    
    // VULN-THREAD-STARVATION FIX: Per-phone locks instead of singleton lock
    // Using ConcurrentHashMap to store individual locks per phone number
    // This prevents one phone's OTP verification from blocking all other users
    private final Map<String, Object> phoneLocks = new ConcurrentHashMap<>();

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
        // VULN-M02 & VULN-THREAD-STARVATION FIX: Lock per phone, not entire service
        // Get or create a lock object specific to this phone number
        // This allows concurrent OTP verification for different phone numbers
        Object lock = phoneLocks.computeIfAbsent(phone, k -> new Object());
        
        synchronized (lock) {
            AttemptData attempts = attemptStore.computeIfAbsent(phone, k -> new AttemptData());
            if (attempts.count.get() >= MAX_ATTEMPTS) {
                otpStore.remove(phone);
                attemptStore.remove(phone);
                phoneLocks.remove(phone); // VULN #13 FIX: Cleanup lock on max attempts
                throw new ValidationException("Quá nhiều lần thử sai. OTP đã bị khóa. Vui lòng yêu cầu mã mới!");
            }

            OTPData stored = otpStore.get(phone);
            
            // VULN #13 FIX: Cleanup lock when OTP doesn't exist
            if (stored == null) {
                attemptStore.remove(phone);
                phoneLocks.remove(phone);
                return false;
            }

            if (stored.expiry.isBefore(LocalDateTime.now())) {
                otpStore.remove(phone);
                attemptStore.remove(phone);
                phoneLocks.remove(phone); // Cleanup lock
                return false;
            }

            // VULN-H03 FIX: Constant-time comparison — ngăn timing side-channel attack
            boolean match = MessageDigest.isEqual(
                    stored.otp.getBytes(StandardCharsets.UTF_8),
                    otp.getBytes(StandardCharsets.UTF_8));

            if (match) {
                otpStore.remove(phone);
                attemptStore.remove(phone);
                phoneLocks.remove(phone); // Cleanup lock
                return true;
            }

            // Failed attempt - increment counter but keep lock for retry
            // Lock will be cleaned up by scheduled task or on next success/expiry
            attempts.count.incrementAndGet();
            return false;
        }
    }

    @Override
    public String resendOTP(String phone) {
        otpStore.remove(phone);
        attemptStore.remove(phone);
        phoneLocks.remove(phone); // VULN #13 FIX: Cleanup lock on resend
        return generateOTP(phone);
    }

    /** 
     * VULN #13 FIX: Cleanup expired OTP data AND orphaned locks
     * Dọn dẹp OTP và attempt data hết hạn mỗi 10 phút 

     * MEMORY LEAK FIX: Also cleanup phoneLocks map to prevent unbounded growth
     * Problem: phoneLocks grows indefinitely with every unique phone number
     * - Failed OTP attempts don't cleanup locks
     * - Random/spam phone numbers accumulate over time
     * - Eventually causes OutOfMemoryError (delayed DoS)

     * Solution: Remove locks for phones that no longer have active OTP
     */
    @Scheduled(fixedDelay = 600_000)
    public void evictExpired() {
        LocalDateTime now = LocalDateTime.now();
        
        // Remove expired OTP entries
        int removedOtps = 0;
        for (var entry : otpStore.entrySet()) {
            if (entry.getValue().expiry.isBefore(now)) {
                otpStore.remove(entry.getKey());
                removedOtps++;
            }
        }
        
        // Remove attempt data for phones without active OTP
        int removedAttempts = 0;
        for (String phone : attemptStore.keySet()) {
            if (!otpStore.containsKey(phone)) {
                attemptStore.remove(phone);
                removedAttempts++;
            }
        }
        
        // VULN #13 FIX: Remove orphaned locks to prevent memory leak
        // A lock is orphaned if there's no active OTP for that phone
        int removedLocks = 0;
        for (String phone : phoneLocks.keySet()) {
            if (!otpStore.containsKey(phone)) {
                phoneLocks.remove(phone);
                removedLocks++;
            }
        }
        
    }

    private record OTPData(String otp, LocalDateTime expiry) {
    }

    private static class AttemptData {
        final AtomicInteger count = new AtomicInteger(0);
    }
}
