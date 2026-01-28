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
    public Account authenticate(String username, String password) {
        System.out.println("=== DEBUG: Authenticate called ===");
        System.out.println("Username: " + username);
        System.out.println("Password: " + password);

        Account account = dao.findById(username).orElse(null);

        if (account == null) {
            System.out.println("DEBUG: Account not found!");
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

    @Override
    public Account createAccount(Account account, MultipartFile photoFile) {
        // Validation - check username exists
        if (existsByUsername(account.getUsername())) {
            throw new ValidationException("username", "Tên đăng nhập đã tồn tại!");
        }

        // Validation - check email exists
        if (existsByEmail(account.getEmail())) {
            throw new ValidationException("email", "Email đã được sử dụng!");
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
}
