package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.dto.response.AuthResult;
import com.springboot.jenka_coffee.entity.Account;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AccountService {
    Account findById(String username);

    List<Account> findAll();

    /**
     * Paginated account list — avoids loading entire table into memory.
     */
    Page<Account> findAllPaginated(Pageable pageable);

    List<Account> getAdministrators();

    Account save(Account account);

    void delete(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /**
     * Authenticate and return a rich result — avoids N+1 query in controller.
     * Distinguishes between wrong credentials vs. account not activated.
     */
    AuthResult authenticateWithResult(String identifier, String password);

    /**
     * Register a new user account with all default values
     * Handles validation, password hashing, and default setup
     */
    void register(String username, String fullname, String phone, String email, String password);

    // Business operations
    Account createAccount(Account account, MultipartFile photoFile);

    Account updateAccount(String username, Account updatedAccount, MultipartFile photoFile);

    boolean canDeleteAccount(String username);

    // New methods for clean controller pattern
    /**
     * Find account by username, throws ResourceNotFoundException if not found
     */
    Account findByIdOrThrow(String username);

    /**
     * Delete account with business rule validation, throws BusinessRuleException if
     * cannot delete
     */
    void deleteOrThrow(String username);

    /**
     * Toggle account activation status
     * 
     * @param username Account username
     * @return Updated account
     */
    Account toggleActivation(String username);

    /**
     * Set admin role for an account (super-admin only).
     * Dedicated endpoint to change admin flag safely.
     *
     * @param username  Account username
     * @param isAdmin   true = grant admin, false = revoke admin
     * @return Updated account
     */
    Account setAdminRole(String username, boolean isAdmin);

    /*
     * Lock account (admin function)
     * 
     * @param username Account username
     * @return Updated account
     */

    /*
     * Unlock account (admin function)
     * 
     * @param username Account username
     * @return Updated account
     */

    /**
     * Admin reset password for user (admin function)
     * 
     * @param username Account username
     * @param newPassword New password (plain text, will be hashed)
     */
    void adminResetPassword(String username, String newPassword);

    // ===== ACCOUNT ACTIVATION & PASSWORD RESET =====

    /*
     * Activate account using activation token
     * 
     * @param token Activation token
     */
    void activateAccount(String token);

    /**
     * Resend activation email/SMS
     * 
     * @param username Account username
     */
    void resendActivation(String username);

    /*
     * Request password reset
     * 
     * @return "EMAIL" or "PHONE" to indicate sending method
     */

    /**
     * Reset password using reset token
     * 
     * @param token       Reset token
     * @param newPassword New password
     */
    void resetPassword(String token, String newPassword);

    /**
     * Verify OTP for phone activation
     * 
     * @param phone Phone number
     * @param otp   OTP code
     */
    void verifyPhoneOTP(String phone, String otp);

    // ===== GOOGLE OAUTH =====
    
    /**
     * Find account by email
     * @param email Email address
     * @return Account or null if not found
     */
    Account findByEmail(String email);
    
    /**
     * Update phone number for account
     * @param username Account username
     * @param phone Phone number
     */
    void updatePhone(String username, String phone);
    
    // ===== SECURITY LAYER METHODS =====
    
    /**
     * Get account security info for JWT validation
     * Used by Security layer to verify account status without direct repository access
     * 
     * @param username Account username
     * @return AccountSecurityInfo containing activation status, admin status, and password reset date
     */
    AccountSecurityInfo getAccountSecurityInfo(String username);

    void requestPasswordReset(@NotBlank(message = "Vui lòng nhập email hoặc số điện thoại đã đăng ký") String identifier);

    /**
         * DTO for security information needed by JWT filter
         */
        record AccountSecurityInfo(boolean exists, @Getter boolean activated, @Getter boolean admin,
                                   @Getter Long lastPasswordResetTimestamp) {

        public static AccountSecurityInfo notFound() {
                return new AccountSecurityInfo(false, false, false, null);
            }

        public static AccountSecurityInfo fromAccount(Account account) {
                Long resetTimestamp = null;
                if (account.getLastPasswordResetDate() != null) {
                    resetTimestamp = account.getLastPasswordResetDate()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli();
                }
                return new AccountSecurityInfo(
                        true,
                        Boolean.TRUE.equals(account.getActivated()),
                        Boolean.TRUE.equals(account.getAdmin()),
                        resetTimestamp
                );
            }
        }
}
