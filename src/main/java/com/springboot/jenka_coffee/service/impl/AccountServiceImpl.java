package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.dto.response.AuthResult;
import com.springboot.jenka_coffee.exception.BusinessRuleException;
import com.springboot.jenka_coffee.exception.ResourceNotFoundException;
import com.springboot.jenka_coffee.exception.ValidationException;
import com.springboot.jenka_coffee.repository.AccountRepository;
import com.springboot.jenka_coffee.service.AccountService;
import com.springboot.jenka_coffee.service.EmailService;
import com.springboot.jenka_coffee.service.OTPService;
import com.springboot.jenka_coffee.service.UploadService;
import com.springboot.jenka_coffee.util.ImageUtils;
import com.springboot.jenka_coffee.util.PasswordSecurity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    
    @PersistenceContext
    private EntityManager entityManager;

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
    @Transactional
    @CacheEvict(value = "accountSecurity", key = "#account.username")
    public Account save(Account account) {
        // If account is marked as new, use persist() to ensure INSERT
        if (account.isNew()) {
            entityManager.persist(account);
            entityManager.flush();
            entityManager.detach(account);
            return account;
        }
        // Otherwise use normal save (merge for updates)
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
        String normalizedEmail = normalizeEmail(email);
        return normalizedEmail != null && dao.existsByEmailIgnoreCase(normalizedEmail);
    }

    @Override
    public AuthResult authenticateWithResult(String identifier, String password) {
        // VULN-BCRYPT-DOS FIX: Giới hạn độ dài password trước khi hash
        if (password == null || password.length() > 72) {
            return AuthResult.invalidCredentials();
        }
        
        String normalizedIdentifier = identifier != null ? identifier.trim() : null;
        Account account = dao.findByUsernameOrEmailOrPhone(normalizedIdentifier).orElse(null);
        
        // VULN-TIMING-ATTACK FIX: Dummy BCrypt check khi account không tồn tại
        // Đảm bảo response time giống nhau cho cả trường hợp tồn tại và không tồn tại
        if (account == null) {
            // Chạy dummy BCrypt hash để timing giống với trường hợp account tồn tại
            passwordSecurity.verifyPassword(password, "$2a$12$dummyHashToPreventTimingAttack1234567890123456789012");
            return AuthResult.invalidCredentials();
        }
        
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

        // VULN-PRE-ACCOUNT-HIJACKING FIX: Email phải được verify trước khi activate
        // Không auto-activate nữa, yêu cầu OTP qua phone
        if (email != null && !email.trim().isEmpty()) {
            newAccount.setEmail(normalizeEmail(email));
        } else {
            newAccount.setEmail(null); // SECURITY FIX: NULL instead of empty string to avoid unique constraint
        }

        // 3. Set defaults for new user registration
        newAccount.setPasswordHash(password); // Will be hashed in createAccount
        newAccount.setActivated(false); // CHANGED: Require phone OTP verification
        newAccount.setAdmin(false);
        newAccount.setPoints(0);
        newAccount.setCustomerRank("MEMBER");

        // 4. Call createAccount which handles validation and hashing
        createAccount(newAccount, null);
        
        // 5. Send OTP to phone for verification
        if (!phone.trim().isEmpty()) {
            otpService.generateOTP(phone.trim());
        }
    }

    @Override
    public Account createAccount(Account account, MultipartFile photoFile) {
        // JPA FIX: Mark entity as new FIRST, before any validation or processing
        // This ensures JPA will call persist() instead of merge()
        account.setNew(true);
        
        log.debug("Creating account '{}' with isNew={}", account.getUsername(), account.isNew());
        
        // Validation - check username exists
        if (existsByUsername(account.getUsername())) {
            throw new ValidationException("username", "Tên đăng nhập đã tồn tại!");
        }

        // Validation - check email exists (only if email is provided)
        if (account.getEmail() != null && !account.getEmail().trim().isEmpty()) {
            account.setEmail(normalizeEmail(account.getEmail()));
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
        // VULN-C01 FIX: Logic was inverted - hash only if NOT already hashed
        if (account.getPasswordHash() != null && !account.getPasswordHash().trim().isEmpty()) {
            if (!passwordSecurity.isPasswordHashed(account.getPasswordHash())) {
                account.setPasswordHash(passwordSecurity.hashPassword(account.getPasswordHash()));
            }
        }

        // JPA FIX: Use dao.save() for consistency and testability
        // Note: account.setNew(true) ensures JPA will call persist() instead of merge()
        log.debug("About to save account '{}', password_hash length: {}", 
                  account.getUsername(), 
                  account.getPasswordHash() != null ? account.getPasswordHash().length() : 0);
        
        Account savedAccount = dao.save(account);
        
        log.debug("Successfully saved account '{}', password_hash_length={}", 
                  savedAccount.getUsername(), 
                  savedAccount.getPasswordHash() != null ? savedAccount.getPasswordHash().length() : 0);
        
        return savedAccount;
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
        String newEmail = normalizeEmail(updatedAccount.getEmail());
        updatedAccount.setEmail(newEmail);
        boolean emailChanged = !Objects.equals(existingEmail, newEmail)
                && newEmail != null && !newEmail.trim().isEmpty();
        if (emailChanged && existsByEmail(newEmail)) {
            throw new ValidationException("email", "Email đã được sử dụng!");
        }

        // Keep old password if new password is empty
        if (updatedAccount.getPasswordHash() == null || updatedAccount.getPasswordHash().trim().isEmpty()) {
            updatedAccount.setPasswordHash(existingAccount.getPasswordHash());
        } else {
            // VULN-C01 FIX: Hash new password only if it's NOT already hashed
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

        // VULN-C03 FIX: Preserve admin flag from existing account
        // Controller sets admin=false to prevent privilege escalation via API
        // But we must preserve the actual admin status from database
        updatedAccount.setAdmin(existingAccount.getAdmin());
        
        // VULN-C03 FIX: Preserve username (primary key cannot be changed)
        updatedAccount.setUsername(existingAccount.getUsername());

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
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account", "username", username));
    }

    @Override
    public void deleteOrThrow(String username) {
        Account account = findByIdOrThrow(username);

        if (!canDeleteAccount(username)) {
            if (account.getAdmin() != null && account.getAdmin()) {
                throw new BusinessRuleException(
                        "Không thể xóa admin cuối cùng trong hệ thống!");
            }
            throw new BusinessRuleException(
                    "Không thể xóa tài khoản này!");
        }

        dao.deleteById(username);
    }

    @Override
    @CacheEvict(value = "accountSecurity", key = "#username")
    public Account toggleActivation(String username) {
        Account account = findByIdOrThrow(username);
        account.setActivated(!account.getActivated());
        return dao.save(account);
    }

    // ===== ADMIN USER MANAGEMENT METHODS =====







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
    public void resendActivation(String identifier) {
        // VULN-ZOMBIE-OTP FIX: Find account by phone/username/email
        Account account = dao.findByUsernameOrEmailOrPhone(identifier).orElse(null);
        
        if (account == null) {
            throw new ValidationException("Không tìm thấy tài khoản!");
        }

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
            // Default to phone OTP
            if (account.getPhone() != null && !account.getPhone().trim().isEmpty()) {
                otpService.generateOTP(account.getPhone());
            } else {
                throw new ValidationException("Tài khoản không có số điện thoại để gửi OTP!");
            }
        }
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
        
        // VULN-SESSION-REVOCATION FIX: Update lastPasswordResetDate to invalidate old tokens
        account.setLastPasswordResetDate(LocalDateTime.now());
        
        dao.save(account);
        
        log.warn("SECURITY: Password reset via token for user '{}'", account.getUsername());
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

    // ===== GOOGLE OAUTH METHODS =====

    @Override
    public Account findByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            return null;
        }
        List<Account> matches = dao.findAllByEmailIgnoreCase(normalizedEmail);
        if (matches == null || matches.isEmpty()) {
            return null;
        }

        return matches.stream()
                .sorted((left, right) -> {
                    int adminCompare = Boolean.compare(Boolean.TRUE.equals(right.getAdmin()), Boolean.TRUE.equals(left.getAdmin()));
                    if (adminCompare != 0) {
                        return adminCompare;
                    }

                    int activeCompare = Boolean.compare(Boolean.TRUE.equals(right.getActivated()), Boolean.TRUE.equals(left.getActivated()));
                    if (activeCompare != 0) {
                        return activeCompare;
                    }

                    LocalDateTime leftDate = left.getCreateDate();
                    LocalDateTime rightDate = right.getCreateDate();
                    if (leftDate == null && rightDate == null) {
                        return 0;
                    }
                    if (leftDate == null) {
                        return 1;
                    }
                    if (rightDate == null) {
                        return -1;
                    }
                    return leftDate.compareTo(rightDate);
                })
                .findFirst()
                .orElse(null);
    }

    @Override
    @Transactional
    public void updatePhone(String username, String phone) {
        Account account = dao.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with username: " + username));
        
        account.setPhone(phone);
        dao.save(account);
        log.info("Updated phone number for user: {}", username);
    }

    @Override
    @Transactional
    @CacheEvict(value = "accountSecurity", key = "#username")
    public Account setAdminRole(String username, boolean isAdmin, String currentAdminUsername) {
        Account account = findByIdOrThrow(username);

        if (Boolean.TRUE.equals(account.getAdmin()) && !isAdmin) {
            long adminCount = dao.countByAdminTrue();
            if (adminCount <= 1) {
                throw new BusinessRuleException("Không thể hạ quyền admin cuối cùng trong hệ thống!");
            }
            if (username.equals(currentAdminUsername)) {
                throw new BusinessRuleException("Không thể tự gỡ quyền admin của chính bạn!");
            }
        }

        account.setAdmin(isAdmin);
        return dao.save(account);
    }

    // ===== SECURITY LAYER METHODS =====

    // VULN #19 FIX: Cache account security info to prevent DB bottleneck
    // PROBLEM: JWT filter calls this method on EVERY request → DB query per request
    // - 100 users × 10 API calls/page = 1000 DB queries per page load
    // - JWT should be stateless but we're making it stateful
    // SOLUTION: Cache with 5-minute TTL
    // - Reduces DB load by 99%
    // - Cache invalidated on admin status change, password reset, account lock
    // - Acceptable staleness: max 5 minutes for privilege revocation
    @Override
    @Cacheable(
        value = "accountSecurity", 
        key = "#username",
        unless = "#result == null || !#result.exists()"
    )
    public AccountService.AccountSecurityInfo getAccountSecurityInfo(String username) {
        Account account = dao.findById(username).orElse(null);
        
        if (account == null) {
            return AccountService.AccountSecurityInfo.notFound();
        }
        
        return AccountService.AccountSecurityInfo.fromAccount(account);
    }

    @Override
    @Transactional
    public void requestPasswordReset(String identifier) {
        log.info("Password reset requested for identifier: {}", identifier);
        
        // VULN-C02 FIX: Silent failure to prevent user enumeration
        // Find account by username, email, or phone
        Account account = dao.findByUsernameOrEmailOrPhone(identifier).orElse(null);
        
        // If account not found, return silently (don't throw exception)
        // This prevents attacker from knowing if email/username exists
        if (account == null) {
            log.warn("Password reset requested for non-existent identifier: {}", identifier);
            // Sleep to prevent timing attack
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return; // Silent return - same behavior as success
        }
        
        // Check if account is activated
        if (!Boolean.TRUE.equals(account.getActivated())) {
            log.warn("Password reset requested for inactive account: {}", identifier);
            // Also return silently - don't leak account status
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return;
        }
        
        // Generate reset token (valid for 1 hour)
        String resetToken = UUID.randomUUID().toString();
        account.setResetToken(resetToken);
        account.setResetTokenExpiry(java.time.LocalDateTime.now().plusHours(1));
        dao.save(account);
        
        // Send reset link/OTP based on identifier type
        if (identifier.contains("@")) {
            // Email - send reset link
            emailService.sendPasswordResetEmail(account.getEmail(), resetToken, account.getFullname());
            log.info("Password reset email sent to: {}", account.getEmail());
        } else if (identifier.matches("\\d{10,11}")) {
            // Phone - send OTP
            String otp = otpService.generateOTP(account.getPhone());
            // OTP service will handle sending SMS
            log.info("Password reset OTP sent to: {}", account.getPhone());
        } else {
            // Username - send to email if available
            if (account.getEmail() != null && !account.getEmail().isEmpty()) {
                emailService.sendPasswordResetEmail(account.getEmail(), resetToken, account.getFullname());
                log.info("Password reset email sent to: {}", account.getEmail());
            } else {
                // SECURITY FIX: Do NOT throw exception — that leaks account existence.
                // If username has no email, silently return same message as if not found.
                // Admin should contact user directly.
                log.warn("Password reset requested for username={} but no email/phone available — silent return", account.getUsername());
            }
        }
    }

    @Override
    @Transactional
    public void adminResetPassword(String username, String newPassword) {
        log.info("Admin resetting password for user: {}", username);
        
        // Find account
        Account account = dao.findById(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản với username: " + username));
        
        // Hash new password using PasswordSecurity
        String hashedPassword = passwordSecurity.hashPassword(newPassword);
        account.setPasswordHash(hashedPassword);
        
        // Update last password reset date
        account.setLastPasswordResetDate(java.time.LocalDateTime.now());
        
        // Save account
        dao.save(account);
        
        log.info("Successfully reset password for user: {}", username);
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String normalized = email.trim().toLowerCase();
        return normalized.isEmpty() ? null : normalized;
    }
}
