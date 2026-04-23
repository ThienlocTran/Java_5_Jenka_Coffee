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
@Transactional
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
        
        // VULN-IDENTITY-SPOOFING FIX: Reset phoneVerified when phone changes
        if (StringUtils.hasText(request.getPhone())) {
            String newPhone = request.getPhone().trim();
            String oldPhone = account.getPhone();
            
            // If phone is changing, reset verification status
            if (!newPhone.equals(oldPhone)) {
                account.setPhone(newPhone);
                account.setPhoneVerified(false);
                log.warn("SECURITY: Phone changed for user '{}', phoneVerified reset to false", username);
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

    @Override
    public Account updateAvatar(String username, MultipartFile avatarFile) {
        Account account = getProfile(username);

        // VULN-ORPHANED-STORAGE FIX: Delete old avatar before uploading new one
        String oldAvatarUrl = account.getPhoto();
        
        // Upload directly to Cloudinary — no local resize needed
        String avatarUrl = uploadService.saveImage(avatarFile);
        if (avatarUrl == null) {
            throw new BusinessRuleException("Không thể tải lên ảnh đại diện");
        }

        account.setPhoto(avatarUrl);
        Account savedAccount = accountRepository.save(account);
        
        // Delete old avatar after successful update (async - don't block response)
        if (oldAvatarUrl != null && !oldAvatarUrl.isEmpty()) {
            try {
                uploadService.deleteImage(oldAvatarUrl);
            } catch (Exception e) {
                log.warn("Failed to delete old avatar for user {}: {}", username, e.getMessage());
                // Don't throw - deletion failure shouldn't block the update
            }
        }
        
        log.info("Updated avatar for user: {}", username);
        return savedAccount;
    }

    @Override
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
        
        // Validate phone
        if (StringUtils.hasText(request.getPhone())) {
            String phone = request.getPhone().trim();
            if (!phone.matches("^[0-9]{10,11}$")) {
                throw new ValidationException("Số điện thoại phải có 10-11 chữ số");
            }
            
            // VULN-IDENTITY-SPOOFING FIX: Check if new phone is different from current
            String currentPhone = currentAccount.getPhone();
            boolean phoneChanging = !phone.equals(currentPhone);
            
            // Check if phone already exists (excluding current user)
            if (accountRepository.existsByPhone(phone) && phoneChanging) {
                throw new ValidationException("Số điện thoại đã được sử dụng bởi tài khoản khác");
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
}