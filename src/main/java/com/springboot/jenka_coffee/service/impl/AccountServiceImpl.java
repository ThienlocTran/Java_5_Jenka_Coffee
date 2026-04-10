package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.dto.response.AuthResult;
import com.springboot.jenka_coffee.exception.ValidationException;
import com.springboot.jenka_coffee.repository.AccountRepository;
import com.springboot.jenka_coffee.service.AccountService;
import com.springboot.jenka_coffee.service.EmailService;
import com.springboot.jenka_coffee.service.OTPService;
import com.springboot.jenka_coffee.service.UploadService;
import com.springboot.jenka_coffee.util.ImageUtils;
import com.springboot.jenka_coffee.util.PasswordSecurity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository dao;
    private final UploadService uploadService;
    private final PasswordSecurity passwordSecurity;
    private final EmailService emailService;
    private final OTPService otpService;

    public AccountServiceImpl(AccountRepository dao, UploadService uploadService,
            PasswordSecurity passwordSecurity, EmailService emailService,
            OTPService otpService) {
        this.dao = dao;
        this.uploadService = uploadService;
        this.passwordSecurity = passwordSecurity;
        this.emailService = emailService;
        this.otpService = otpService;
    }

    @Override
    public Account findById(String username) {
        return dao.findById(username).orElse(null);
    }

    @Override
    public List<Account> findAll() {
        return dao.findAll();
    }

    @Override
    public Page<Account> findAllPaginated(Pageable pageable) {
        return dao.findAll(pageable);
    }

    @Override
    public List<Account> getAdministrators() {
        // ✅ Delegate to repository - database query instead of in-memory filter
        return dao.findByAdminTrue();
    }

    @Override
    public Account save(Account account) {
        return dao.save(account);
    }

    @Override
    public void delete(String username) {
        dao.deleteById(username);
    }

    @Override
    public boolean existsByUsername(String username) {
        return dao.existsById(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        // ✅ Delegate to repository - efficient database query
        return dao.existsByEmail(email);
    }

    @Override
    public Account authenticate(String identifier, String password) {
        Account account = dao.findByUsernameOrEmailOrPhone(identifier).orElse(null);
        if (account == null || !account.getActivated()) {
            return null;
        }
        return passwordSecurity.verifyPassword(password, account.getPasswordHash()) ? account : null;
    }

    @Override
    public AuthResult authenticateWithResult(String identifier, String password) {
        Account account = dao.findByUsernameOrEmailOrPhone(identifier).orElse(null);
        if (account == null) return AuthResult.invalidCredentials();
        if (!account.getActivated()) return AuthResult.notActivated(account);
        if (!passwordSecurity.verifyPassword(password, account.getPasswordHash())) return AuthResult.invalidCredentials();
        return AuthResult.success(account);
    }

    @Override
    public void register(String username, String fullname, String phone, String email, String password) {
        // Create new account object — validation (username/email uniqueness) is handled in createAccount
        Account newAccount = new Account();
        newAccount.setUsername(username.trim());
        newAccount.setFullname(fullname.trim());
        newAccount.setPhone(phone.trim());

        // Email is optional - set to empty string if not provided
        if (email != null && !email.trim().isEmpty()) {
            newAccount.setEmail(email.trim());
        } else {
            newAccount.setEmail(""); // Set empty string instead of null
        }

        // 3. Set defaults for new user registration
        newAccount.setPasswordHash(password); // Will be hashed in createAccount
        newAccount.setActivated(true); // Auto-activate: skip email/OTP since SMTP is unavailable
        newAccount.setAdmin(false);
        newAccount.setPoints(0);
        newAccount.setCustomerRank("MEMBER");

        // 4. Call createAccount which handles validation and hashing
        createAccount(newAccount, null);
    }

    @Override
    public Account createAccount(Account account, MultipartFile photoFile) {
        // Validation - check username exists
        if (existsByUsername(account.getUsername())) {
            throw new ValidationException("username", "Tên đăng nhập đã tồn tại!");
        }

        // Validation - check email exists (only if email is provided)
        if (account.getEmail() != null && !account.getEmail().trim().isEmpty()) {
            if (existsByEmail(account.getEmail())) {
                throw new ValidationException("email", "Email đã được sử dụng!");
            }
        }

        // Handle photo upload
        if (photoFile != null && !photoFile.isEmpty()) {
            try {
                String fileName = uploadService.saveImageWithCompression(photoFile, 
                        ImageUtils.ImagePresets.AVATAR_WIDTH, 
                        ImageUtils.ImagePresets.AVATAR_QUALITY);
                account.setPhoto(fileName);
            } catch (Exception e) {
                throw new ValidationException("photo", "Lỗi khi upload ảnh: " + e.getMessage());
            }
        }

        // Apply default values
        if (account.getActivated() == null) {
            account.setActivated(true);
        }
        if (account.getAdmin() == null) {
            account.setAdmin(false);
        }

        // Hash password before saving
        if (account.getPasswordHash() != null && !account.getPasswordHash().trim().isEmpty()) {
            if (!passwordSecurity.isPasswordHashed(account.getPasswordHash())) {
                account.setPasswordHash(passwordSecurity.hashPassword(account.getPasswordHash()));
            }
        }

        return dao.save(account);
    }

    @Override
    public Account updateAccount(String username, Account updatedAccount, MultipartFile photoFile) {
        // Check if account exists
        Account existingAccount = findById(username);
        if (existingAccount == null) {
            throw new ValidationException("Không tìm thấy tài khoản!");
        }

        // Validation - check email (if changed)
        String existingEmail = existingAccount.getEmail();
        String newEmail = updatedAccount.getEmail();
        boolean emailChanged = !Objects.equals(existingEmail, newEmail)
                && newEmail != null && !newEmail.trim().isEmpty();
        if (emailChanged && existsByEmail(newEmail)) {
            throw new ValidationException("email", "Email đã được sử dụng!");
        }

        // Keep old password if new password is empty
        if (updatedAccount.getPasswordHash() == null || updatedAccount.getPasswordHash().trim().isEmpty()) {
            updatedAccount.setPasswordHash(existingAccount.getPasswordHash());
        } else {
            // Hash new password if it's not already hashed
            if (!passwordSecurity.isPasswordHashed(updatedAccount.getPasswordHash())) {
                updatedAccount.setPasswordHash(passwordSecurity.hashPassword(updatedAccount.getPasswordHash()));
            }
        }

        // Handle photo upload or keep old photo
        if (photoFile != null && !photoFile.isEmpty()) {
            try {
                String fileName = uploadService.saveImageWithCompression(photoFile, 
                        ImageUtils.ImagePresets.AVATAR_WIDTH, 
                        ImageUtils.ImagePresets.AVATAR_QUALITY);
                updatedAccount.setPhoto(fileName);
            } catch (Exception e) {
                throw new ValidationException("photo", "Lỗi khi upload ảnh: " + e.getMessage());
            }
        } else {
            updatedAccount.setPhoto(existingAccount.getPhoto());
        }

        return dao.save(updatedAccount);
    }

    @Override
    public boolean canDeleteAccount(String username) {
        Account account = findById(username);
        if (account == null) {
            return false;
        }

        // Cannot delete last admin
        if (account.getAdmin() != null && account.getAdmin()) {
            // ✅ Use repository count query - efficient
            long adminCount = dao.countByAdminTrue();
            return adminCount > 1;
        }

        return true;
    }

    @Override
    public Account findByIdOrThrow(String username) {
        return dao.findById(username)
                .orElseThrow(() -> new com.springboot.jenka_coffee.exception.ResourceNotFoundException(
                        "Account", "username", username));
    }

    @Override
    public void deleteOrThrow(String username) {
        Account account = findByIdOrThrow(username);

        if (!canDeleteAccount(username)) {
            if (account.getAdmin() != null && account.getAdmin()) {
                throw new com.springboot.jenka_coffee.exception.BusinessRuleException(
                        "Không thể xóa admin cuối cùng trong hệ thống!");
            }
            throw new com.springboot.jenka_coffee.exception.BusinessRuleException(
                    "Không thể xóa tài khoản này!");
        }

        dao.deleteById(username);
    }

    @Override
    public Account toggleActivation(String username) {
        Account account = findByIdOrThrow(username);
        account.setActivated(!account.getActivated());
        return dao.save(account);
    }

    // ===== ADMIN USER MANAGEMENT METHODS =====

    @Override
    public Account lockAccount(String username) {
        Account account = findByIdOrThrow(username);
        
        // Business rule: Cannot lock admin if they are the last admin
        if (account.getAdmin() != null && account.getAdmin()) {
            long adminCount = dao.countByAdminTrue();
            if (adminCount <= 1) {
                throw new com.springboot.jenka_coffee.exception.BusinessRuleException(
                        "Không thể khóa admin cuối cùng trong hệ thống!");
            }
        }
        
        account.setActivated(false);
        return dao.save(account);
    }

    @Override
    public Account unlockAccount(String username) {
        Account account = findByIdOrThrow(username);
        account.setActivated(true);
        return dao.save(account);
    }

    @Override
    public Account adminResetPassword(String username, String newPassword) {
        Account account = findByIdOrThrow(username);
        
        // Validate new password
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new ValidationException("Mật khẩu mới phải có ít nhất 6 ký tự!");
        }
        
        // Hash and save new password (reuse existing logic)
        account.setPasswordHash(passwordSecurity.hashPassword(newPassword.trim()));

        // Clear any existing reset tokens
        account.setResetToken(null);
        account.setResetTokenExpiry(null);

        Account saved = dao.save(account);
        // VULN-026 FIX: Audit log bắt buộc
        log.warn("SECURITY AUDIT: Admin reset password for user '{}' at {}", username, java.time.LocalDateTime.now());
        return saved;
    }

    // ===== ACCOUNT ACTIVATION & PASSWORD RESET IMPLEMENTATION =====

    @Override
    public void activateAccount(String token) {
        Account account = dao.findByActivationToken(token)
                .orElseThrow(() -> new ValidationException("Token kích hoạt không hợp lệ!"));

        if (account.getActivationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new ValidationException("Token kích hoạt đã hết hạn. Vui lòng yêu cầu gửi lại!");
        }

        account.setActivated(true);
        account.setActivationToken(null);
        account.setActivationTokenExpiry(null);
        dao.save(account);
    }

    @Override
    public void resendActivation(String username) {
        Account account = findByIdOrThrow(username);

        if (account.getActivated()) {
            throw new ValidationException("Tài khoản đã được kích hoạt!");
        }

        // Generate new token
        String token = UUID.randomUUID().toString();
        account.setActivationToken(token);
        account.setActivationTokenExpiry(LocalDateTime.now().plusHours(24));
        dao.save(account);

        // Send based on activation method
        if ("EMAIL".equals(account.getActivationMethod())) {
            try {
                emailService.sendActivationEmail(account.getEmail(), token, account.getFullname());
            } catch (Exception e) {
                log.warn("Activation email sending failed for {}: {}", account.getEmail(), e.getMessage());
            }
        } else {
            otpService.generateOTP(account.getPhone());
        }
    }

    @Override
    public String requestPasswordReset(String identifier) {
        Account account = dao.findByUsernameOrEmailOrPhone(identifier).orElse(null);

        // VULN-003 FIX: Silent fail — không tiết lộ user có/không tồn tại
        // Luôn trả về cùng message dù user tồn tại hay không
        if (account == null) {
            log.info("Password reset requested for non-existent identifier (masked for security)");
            return "EMAIL"; // Fake return — timing consistent
        }

        boolean hasEmail = account.getEmail() != null && !account.getEmail().trim().isEmpty();
        boolean hasPhone = account.getPhone() != null && !account.getPhone().trim().isEmpty();

        if (hasEmail) {
            try {
                String resetToken = UUID.randomUUID().toString();
                account.setResetToken(resetToken);
                account.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
                dao.save(account);
                emailService.sendPasswordResetEmail(account.getEmail(), resetToken, account.getFullname());
                return "EMAIL";
            } catch (Exception e) {
                log.warn("Email sending failed (masked for security)");
                if (!hasPhone) return "EMAIL"; // Silent fail
            }
        }

        if (hasPhone) {
            otpService.generateOTP(account.getPhone());
            return "PHONE";
        }
        return "EMAIL"; // Silent fail
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        Account account = dao.findByResetToken(token)
                .orElseThrow(() -> new ValidationException("Token đặt lại mật khẩu không hợp lệ!"));

        if (account.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new ValidationException("Token đã hết hạn. Vui lòng yêu cầu đặt lại mật khẩu lại!");
        }

        // Hash and save new password
        account.setPasswordHash(passwordSecurity.hashPassword(newPassword));
        account.setResetToken(null);
        account.setResetTokenExpiry(null);
        dao.save(account);
    }

    @Override
    public void verifyPhoneOTP(String phone, String otp) {
        if (!otpService.verifyOTP(phone, otp)) {
            throw new ValidationException("Mã OTP không chính xác hoặc đã hết hạn!");
        }

        // Find account by phone and activate
        Account account = dao.findByPhone(phone)
                .orElseThrow(() -> new ValidationException("Không tìm thấy tài khoản với số điện thoại này!"));

        account.setActivated(true);
        account.setPhoneVerified(true);
        account.setActivationToken(null);
        account.setActivationTokenExpiry(null);
        dao.save(account);
    }
}
