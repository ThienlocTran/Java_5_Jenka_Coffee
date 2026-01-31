package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.exception.ValidationException;
import com.springboot.jenka_coffee.repository.AccountRepository;
import com.springboot.jenka_coffee.service.AccountService;
import com.springboot.jenka_coffee.service.UploadService;
import com.springboot.jenka_coffee.util.PasswordSecurity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository dao;
    private final UploadService uploadService;
    private final PasswordSecurity passwordSecurity;

    public AccountServiceImpl(AccountRepository dao, UploadService uploadService, PasswordSecurity passwordSecurity) {
        this.dao = dao;
        this.uploadService = uploadService;
        this.passwordSecurity = passwordSecurity;
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
    public List<Account> getAdministrators() {
        return dao.findAll().stream()
                .filter(acc -> acc.getAdmin() != null && acc.getAdmin())
                .toList();
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
        return dao.findAll().stream()
                .anyMatch(acc -> acc.getEmail().equalsIgnoreCase(email));
    }

    @Override
    public Account authenticate(String identifier, String password) {
        System.out.println("=== DEBUG: Authenticate called ===");
        System.out.println("Identifier: " + identifier);
        System.out.println("Password: " + password);

        // Try to find account by username, email, or phone
        Account account = findByUsernameOrEmailOrPhone(identifier);

        if (account == null) {
            System.out.println("DEBUG: Account not found with identifier: " + identifier);
            return null; // User not found
        }

        System.out.println("DEBUG: Account found - " + account.getUsername());
        System.out.println("DEBUG: Password hash: " + account.getPasswordHash());
        System.out.println(
                "DEBUG: Hash length: " + (account.getPasswordHash() != null ? account.getPasswordHash().length() : 0));

        if (!account.getActivated()) {
            System.out.println("DEBUG: Account not activated!");
            return null; // Account deactivated
        }

        // BCrypt password verification
        boolean passwordMatch = passwordSecurity.verifyPassword(password, account.getPasswordHash());
        System.out.println("DEBUG: Password match: " + passwordMatch);

        if (passwordMatch) {
            System.out.println("DEBUG: Authentication SUCCESS!");
            return account;
        }

        System.out.println("DEBUG: Authentication FAILED - wrong password");
        return null; // Wrong password
    }

    /**
     * Tìm account theo username, email, hoặc phone
     * Hỗ trợ đăng nhập linh hoạt
     */
    private Account findByUsernameOrEmailOrPhone(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return null;
        }

        String cleanIdentifier = identifier.trim();

        // Thử tìm theo username trước
        Account account = dao.findById(cleanIdentifier).orElse(null);
        if (account != null) {
            return account;
        }

        // Nếu không tìm thấy, thử tìm theo email hoặc phone
        return dao.findAll().stream()
                .filter(acc -> {
                    // Kiểm tra email (case-insensitive)
                    if (acc.getEmail() != null && acc.getEmail().equalsIgnoreCase(cleanIdentifier)) {
                        return true;
                    }
                    // Kiểm tra phone
                    if (acc.getPhone() != null && acc.getPhone().equals(cleanIdentifier)) {
                        return true;
                    }
                    return false;
                })
                .findFirst()
                .orElse(null);
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
                String fileName = uploadService.saveImage(photoFile);
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
        if (!existingAccount.getEmail().equals(updatedAccount.getEmail()) &&
                existsByEmail(updatedAccount.getEmail())) {
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
                String fileName = uploadService.saveImage(photoFile);
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
            List<Account> admins = getAdministrators();
            if (admins.size() <= 1) {
                return false;
            }
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
}
