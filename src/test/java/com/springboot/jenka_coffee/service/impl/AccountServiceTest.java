package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.dto.response.AuthResult;
import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.exception.ValidationException;
import com.springboot.jenka_coffee.repository.AccountRepository;
import com.springboot.jenka_coffee.service.EmailService;
import com.springboot.jenka_coffee.service.OTPService;
import com.springboot.jenka_coffee.service.UploadService;
import com.springboot.jenka_coffee.util.PasswordSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UploadService uploadService;

    @Mock
    private PasswordSecurity passwordSecurity;

    @Mock
    private EmailService emailService;

    @Mock
    private OTPService otpService;

    @InjectMocks
    private AccountServiceImpl accountService;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setUsername("testuser");
        testAccount.setEmail("test@example.com");
        testAccount.setPhone("0123456789");
        testAccount.setPasswordHash("hashed_password");
        testAccount.setActivated(true);
    }

    @Test
    @DisplayName("Test authenticateWithResult - Success")
    void authenticateWithResult_Success() {
        when(accountRepository.findByUsernameOrEmailOrPhone("testuser")).thenReturn(Optional.of(testAccount));
        when(passwordSecurity.verifyPassword("password123", "hashed_password")).thenReturn(true);

        AuthResult result = accountService.authenticateWithResult("testuser", "password123");

        assertTrue(result.isSuccess());
        assertEquals("testuser", result.account().getUsername());
        verify(accountRepository).findByUsernameOrEmailOrPhone("testuser");
    }

    @Test
    @DisplayName("Test authenticateWithResult - Invalid Credentials")
    void authenticateWithResult_InvalidCredentials() {
        when(accountRepository.findByUsernameOrEmailOrPhone("wronguser")).thenReturn(Optional.empty());

        AuthResult result = accountService.authenticateWithResult("wronguser", "password");

        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("Test register - Success")
    void register_Success() {
        when(accountRepository.existsById("newuser")).thenReturn(false);
        when(passwordSecurity.hashPassword("pass123")).thenReturn("hashed_pass");
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArguments()[0]);

        accountService.register("newuser", "New User", "0123456789", "new@example.com", "pass123");

        verify(accountRepository).save(argThat(account -> 
            account.getUsername().equals("newuser") && 
            account.getPasswordHash().equals("hashed_pass")
        ));
        verify(otpService).generateOTP("0123456789");
    }

    @Test
    @DisplayName("Test register - Duplicate Username")
    void register_DuplicateUsername() {
        when(accountRepository.existsById("testuser")).thenReturn(true);

        assertThrows(ValidationException.class, () -> {
            accountService.register("testuser", "Test", "0123", "test@test.com", "pass");
        });
    }

    @Test
    @DisplayName("Test toggleActivation")
    void toggleActivation() {
        testAccount.setActivated(true);
        when(accountRepository.findById("testuser")).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArguments()[0]);

        Account result = accountService.toggleActivation("testuser");

        assertFalse(result.getActivated());
        verify(accountRepository).save(testAccount);
    }

    @Test
    @DisplayName("Test deleteOrThrow - Admin Protection")
    void deleteOrThrow_AdminProtection() {
        testAccount.setAdmin(true);
        when(accountRepository.findById("testuser")).thenReturn(Optional.of(testAccount));
        when(accountRepository.countByAdminTrue()).thenReturn(1L);

        assertThrows(com.springboot.jenka_coffee.exception.BusinessRuleException.class, () -> {
            accountService.deleteOrThrow("testuser");
        });
    }
}
