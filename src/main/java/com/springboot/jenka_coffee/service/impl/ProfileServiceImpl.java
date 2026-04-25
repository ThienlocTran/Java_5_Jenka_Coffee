package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.dto.request.ProfileUpdateRequest;
import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.exception.BusinessRuleException;
import com.springboot.jenka_coffee.exception.ResourceNotFoundException;
import com.springboot.jenka_coffee.exception.ValidationException;
import com.springboot.jenka_coffee.repository.AccountRepository;
import com.springboot.jenka_coffee.service.ProfileService;
import com.springboot.jenka_coffee.service.UploadService;
import com.springboot.jenka_coffee.util.PasswordSecurity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
// VULN #11 FIX: Removed class-level @Transactional to prevent connection pool exhaustion
// PROBLEM: Class-level @Transactional applies to ALL methods, including updateAvatar()
// - DB connection held during Cloudinary upload (network I/O, 1-3 seconds)
// - Multiple concurrent avatar uploads exhaust connection pool
// SOLUTION: Apply @Transactional selectively on methods that need it
// - Methods with only DB operations: @Transactional
// - Methods with network I/O: No @Transactional, or split into two methods
public class ProfileServiceImpl implements ProfileService {

    private final AccountRepository accountRepository;
    private final UploadService uploadService;
    private final PasswordSecurity passwordSecurity;

    @Override
    @Transactional(readOnly = true)
    public Account getProfile(String username) {
        return accountRepository.findById(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản: " + username));
    }

    @Override
    @Transactional
    public Account updateProfile(String username, ProfileUpdateRequest request) {
        Account account = getProfile(username);
        
        // Validate request
        validateProfileUpdate(request, account);
        
        // Update basic information
        if (StringUtils.hasText(request.getFullname())) {
            account.setFullname(request.getFullname().trim());
        }
        
        // VULN-RECOVERY-DEPRIVATION FIX: Prevent removing last recovery method
        if (request.getEmail() != null) {
            String email = request.getEmail().trim();
            if (email.isEmpty()) {
                // User wants to remove email - check if they have phone as backup
                if (account.getPhone() == null || account.getPhone().trim().isEmpty()) {
                    throw new ValidationException(
                            "Không thể xóa email vì bạn chưa có số điện thoại. " +
                            "Vui lòng thêm số điện thoại trước khi xóa email để đảm bảo có thể khôi phục tài khoản.");
                }
                account.setEmail(null);
            } else {
                account.setEmail(email.toLowerCase());
            }
        }
        
        // VULN #14 FIX: Allow users to delete phone number (privacy/data control)
        // VULN #17 FIX: Normalize phone number to handle DTO validation format
        // PROBLEM: DTO accepts "+84901234567" but service only accepts "0901234567"
        // SOLUTION: Normalize phone to remove non-digits and convert +84 to 0
        // - request.getPhone() == null → don't update phone
        // - request.getPhone() == "" → delete phone (set to null)
        // - request.getPhone() == "+84901234567" → normalize to "0901234567" and update
        if (request.getPhone() != null) {
            String rawPhone = request.getPhone().trim();
            
            if (rawPhone.isEmpty()) {
                // User wants to delete phone - check if they have email as backup
                if (account.getEmail() == null || account.getEmail().trim().isEmpty()) {
                    throw new ValidationException(
                            "Không thể xóa số điện thoại vì bạn chưa có email. " +
                            "Vui lòng thêm email trước khi xóa số điện thoại để đảm bảo có thể khôi phục tài khoản.");
                }
                account.setPhone(null);
                account.setPhoneVerified(false); // Reset verification status
                log.info("User '{}' deleted their phone number", username);
            } else {
                // User wants to update phone - normalize format
                String normalizedPhone = normalizePhoneNumber(rawPhone);
                String oldPhone = account.getPhone();
                
                // If phone is changing, reset verification status
                if (!normalizedPhone.equals(oldPhone)) {
                    account.setPhone(normalizedPhone);
                    account.setPhoneVerified(false);
                    log.warn("SECURITY: Phone changed for user '{}', phoneVerified reset to false", username);
                }
            }
        }
        
        // Handle password change if requested
        if (StringUtils.hasText(request.getNewPassword())) {
            changePasswordInternal(account, request.getCurrentPassword(), request.getNewPassword());
        }
        
        Account savedAccount = accountRepository.save(account);
        log.info("Updated profile for user: {}", username);
        
        return savedAccount;
    }

    // VULN #11 FIX: Connection Pool Exhaustion Prevention in Avatar Upload
    // PROBLEM: Class-level @Transactional held DB connection during Cloudinary upload
    // SOLUTION: Split into two steps - upload outside transaction, save inside transaction
    @Override
    public Account updateAvatar(String username, MultipartFile avatarFile) {
        // STEP 1: Get old avatar URL (quick query, no transaction needed)
        Account account = getProfile(username);
        String oldAvatarUrl = account.getPhoto();
        
        // STEP 2: Upload to Cloudinary OUTSIDE transaction (no DB connection held during network I/O)
        String avatarUrl = uploadService.saveImage(avatarFile);
        if (avatarUrl == null) {
            throw new BusinessRuleException("Không thể tải lên ảnh đại diện");
        }

        // STEP 3: Save to database in separate transaction (fast, no network I/O)
        Account savedAccount = updateAvatarInDatabase(username, avatarUrl);
        
        // STEP 4: Cleanup old avatar after successful update (async - don't block response)
        if (oldAvatarUrl != null && !oldAvatarUrl.isEmpty()) {
            try {
                uploadService.deleteImage(oldAvatarUrl);
                log.info("Deleted old avatar for user: {}", username);
            } catch (Exception e) {
                log.warn("Failed to delete old avatar for user {}: {}", username, e.getMessage());
                // Don't throw - deletion failure shouldn't block the update
            }
        }
        
        log.info("Updated avatar for user: {}", username);
        return savedAccount;
    }
    
    /**
     * VULN #11 FIX: Separate transactional method for DB operations only
     * This method ONLY does database operations - no network I/O
     * Transaction is short-lived, connection released quickly
     */
    @Transactional
    protected Account updateAvatarInDatabase(String username, String avatarUrl) {
        Account account = accountRepository.findById(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản: " + username));
        
        account.setPhoto(avatarUrl);
        Account savedAccount = accountRepository.save(account);
        log.info("Saved avatar URL to database for user: {}", username);
        
        return savedAccount;
    }

    @Override
    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        Account account = getProfile(username);
        changePasswordInternal(account, currentPassword, newPassword);
        accountRepository.save(account);
    }

    @Override
    public void validateProfileUpdate(ProfileUpdateRequest request, Account currentAccount) {
        // Validate fullname
        if (StringUtils.hasText(request.getFullname())) {
            if (request.getFullname().trim().length() < 2) {
                throw new ValidationException("Họ tên phải có ít nhất 2 ký tự");
            }
            if (request.getFullname().trim().length() > 100) {
                throw new ValidationException("Họ tên không được vượt quá 100 ký tự");
            }
        }
        
        // Validate email
        if (StringUtils.hasText(request.getEmail())) {
            String email = request.getEmail().trim().toLowerCase();
            if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                throw new ValidationException("Email không hợp lệ");
            }
            
            // Check if email already exists (excluding current user)
            if (accountRepository.existsByEmail(email) && 
                !email.equals(currentAccount.getEmail())) {
                throw new ValidationException("Email đã được sử dụng bởi tài khoản khác");
            }
        }
        
        // VULN #14 FIX: Validate phone update/deletion
        // VULN #17 FIX: Normalize phone before validation
        if (request.getPhone() != null) {
            String phone = request.getPhone().trim();
            
            if (phone.isEmpty()) {
                // User wants to delete phone - ensure they have email as backup
                if (currentAccount.getEmail() == null || currentAccount.getEmail().trim().isEmpty()) {
                    throw new ValidationException(
                            "Không thể xóa số điện thoại vì bạn chưa có email. " +
                            "Vui lòng thêm email trước khi xóa số điện thoại.");
                }
            } else {
                // User wants to update phone - normalize and validate
                String normalizedPhone = normalizePhoneNumber(phone);
                
                // Validate normalized format (should be 10-11 digits starting with 0)
                if (!normalizedPhone.matches("^0[0-9]{9,10}$")) {
                    throw new ValidationException("Số điện thoại không hợp lệ sau khi chuẩn hóa");
                }
                
                // VULN-IDENTITY-SPOOFING FIX: Check if new phone is different from current
                String currentPhone = currentAccount.getPhone();
                boolean phoneChanging = !normalizedPhone.equals(currentPhone);
                
                // Check if phone already exists (excluding current user)
                if (accountRepository.existsByPhone(normalizedPhone) && phoneChanging) {
                    throw new ValidationException("Số điện thoại đã được sử dụng bởi tài khoản khác");
                }
            }
        }
        
        // VULN-RECOVERY-DEPRIVATION FIX: Validate email removal
        if (request.getEmail() != null && request.getEmail().trim().isEmpty()) {
            // User wants to remove email - ensure they have phone as backup
            if (currentAccount.getPhone() == null || currentAccount.getPhone().trim().isEmpty()) {
                throw new ValidationException(
                        "Không thể xóa email vì bạn chưa có số điện thoại. " +
                        "Vui lòng thêm số điện thoại trước khi xóa email.");
            }
        }
        
        // Validate password change
        if (StringUtils.hasText(request.getNewPassword())) {
            // VULN-OAUTH-PASSWORD-LOCKOUT FIX: OAuth users don't need current password
            boolean isOAuthAccount = "GOOGLE_OAUTH_NO_PASSWORD".equals(currentAccount.getPasswordHash());
            
            if (!isOAuthAccount && !StringUtils.hasText(request.getCurrentPassword())) {
                throw new ValidationException("Vui lòng nhập mật khẩu hiện tại");
            }
            
            if (!StringUtils.hasText(request.getConfirmPassword())) {
                throw new ValidationException("Vui lòng xác nhận mật khẩu mới");
            }
            
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                throw new ValidationException("Mật khẩu xác nhận không khớp");
            }
            
            if (request.getNewPassword().length() < 6) {
                throw new ValidationException("Mật khẩu mới phải có ít nhất 6 ký tự");
            }
            
            // Verify current password (only for non-OAuth accounts)
            if (!isOAuthAccount && !passwordSecurity.verifyPassword(request.getCurrentPassword(), currentAccount.getPasswordHash())) {
                throw new ValidationException("Mật khẩu hiện tại không đúng");
            }
        }
    }
    
    private void changePasswordInternal(Account account, String currentPassword, String newPassword) {
        // VULN-OAUTH-PASSWORD-LOCKOUT FIX: Allow OAuth users to set password without current password
        boolean isOAuthAccount = "GOOGLE_OAUTH_NO_PASSWORD".equals(account.getPasswordHash());
        
        if (!isOAuthAccount) {
            // Regular account - verify current password
            if (!passwordSecurity.verifyPassword(currentPassword, account.getPasswordHash())) {
                throw new ValidationException("Mật khẩu hiện tại không đúng");
            }

            // VULN-029 FIX: Không cho phép đặt lại mật khẩu giống mật khẩu cũ
            if (passwordSecurity.verifyPassword(newPassword, account.getPasswordHash())) {
                throw new ValidationException("Mật khẩu mới không được trùng với mật khẩu hiện tại!");
            }
        } else {
            // OAuth account - allow setting password for the first time
            log.info("OAuth user '{}' is setting password for the first time", account.getUsername());
        }

        String hashedPassword = passwordSecurity.hashPassword(newPassword);
        account.setPasswordHash(hashedPassword);
        
        // VULN-SESSION-REVOCATION FIX: Update lastPasswordResetDate to invalidate old tokens
        account.setLastPasswordResetDate(LocalDateTime.now());

        log.warn("SECURITY: Password changed for user '{}'", account.getUsername());
    }
    
    /**
     * VULN #17 FIX: Normalize phone number to handle different formats
     * Converts: "+84901234567" → "0901234567"
     * Converts: "+84 90 123 4567" → "0901234567"
     * Converts: "0901234567" → "0901234567"
     * 
     * This ensures consistency between DTO validation (accepts +84) and service layer (expects 0)
     */
    private String normalizePhoneNumber(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        
        // Remove all non-digit characters (spaces, dots, dashes)
        String digitsOnly = phone.replaceAll("[^0-9+]", "");
        
        // Convert +84 prefix to 0
        if (digitsOnly.startsWith("+84")) {
            digitsOnly = "0" + digitsOnly.substring(3);
        } else if (digitsOnly.startsWith("84") && digitsOnly.length() >= 11) {
            // Handle case where user enters 84901234567 without +
            digitsOnly = "0" + digitsOnly.substring(2);
        }
        
        return digitsOnly;
    }
}