package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.entity.Account;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AccountService {
    Account findById(String username);

    List<Account> findAll();

    List<Account> getAdministrators();

    Account save(Account account);

    void delete(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // Authentication
    Account authenticate(String username, String password);

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
     */
    Account toggleActivation(String username);
}
