package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.dto.response.AuthResult;
import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.exception.BusinessRuleException;
import com.springboot.jenka_coffee.exception.ResourceNotFoundException;
import com.springboot.jenka_coffee.exception.ValidationException;
import com.springboot.jenka_coffee.repository.AccountRepository;
import com.springboot.jenka_coffee.service.impl.AccountServiceImpl;
import com.springboot.jenka_coffee.util.PasswordSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

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
        testAccount.setFullname("Test User");
        testAccount.setEmail("test@example.com");
        testAccount.setPhone("0123456789");
        testAccount.setPasswordHash("$2a$12$hashedPassword");
        testAccount.setActivated(true);
        testAccount.setAdmin(false);
        testAccount.setPoints(0);
        testAccount.setCustomerRank("MEMBER");
    }

    @Test
    // TC-ACC-SER-001: FindById username not exists
    // Expected result: Throw ResourceNotFoundException
    void TC_ACC_SER_001() {
        // Arrange
        when(accountRepository.findById("ghost_user")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            accountService.findByIdOrThrow("ghost_user");
        });

        verify(accountRepository).findById("ghost_user");
    }

    @Test
    // TC-ACC-SER-002: Create account with duplicate username
    // Expected result: Throw ValidationException
    void TC_ACC_SER_002() {
        // Arrange
        Account newAccount = new Account();
        newAccount.setUsername("existing_user");
        newAccount.setEmail("new@test.com");
        newAccount.setPasswordHash("password123");

        when(accountRepository.existsById("existing_user")).thenReturn(true);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            accountService.createAccount(newAccount, null);
        });

        assertEquals("Tên đăng nhập đã tồn tại!", exception.getMessage());
        verify(accountRepository).existsById("existing_user");
        verify(accountRepository, never()).save(any());
    }

    @Test
    // TC-ACC-SER-003: Create account with duplicate email
    // Expected result: Throw ValidationException
    void TC_ACC_SER_003() {
        // Arrange
        Account newAccount = new Account();
        newAccount.setUsername("newuser");
        newAccount.setEmail("existing@test.com");
        newAccount.setPasswordHash("password123");

        when(accountRepository.existsById("newuser")).thenReturn(false);
        when(accountRepository.existsByEmail("existing@test.com")).thenReturn(true);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            accountService.createAccount(newAccount, null);
        });

        assertEquals("Email đã được sử dụng!", exception.getMessage());
        verify(accountRepository).existsById("newuser");
        verify(accountRepository).existsByEmail("existing@test.com");
        verify(accountRepository, never()).save(any());
    }

    @Test
    // TC-ACC-SER-004: DeleteOrThrow username not exists
    // Expected result: Throw ResourceNotFoundException
    void TC_ACC_SER_004() {
        // Arrange
        when(accountRepository.findById("ghost_user")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            accountService.deleteOrThrow("ghost_user");
        });

        verify(accountRepository).findById("ghost_user");
        verify(accountRepository, never()).deleteById(anyString());
    }

    @Test
    // TC-ACC-SER-005: ToggleActivation username not exists
    // Expected result: Throw ResourceNotFoundException
    void TC_ACC_SER_005() {
        // Arrange
        when(accountRepository.findById("ghost_user")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            accountService.toggleActivation("ghost_user");
        });

        verify(accountRepository).findById("ghost_user");
        verify(accountRepository, never()).save(any());
    }

    @Test
    // TC-ACC-SER-006: AdminResetPassword with empty password
    // Expected result: Throw IllegalArgumentException or BusinessRuleException
    void TC_ACC_SER_006() {
        // Arrange
        when(accountRepository.findById("user1")).thenReturn(Optional.of(testAccount));
        when(passwordSecurity.hashPassword("")).thenThrow(new IllegalArgumentException("Password cannot be empty"));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            accountService.adminResetPassword("user1", "");
        });

        verify(accountRepository).findById("user1");
        verify(accountRepository, never()).save(any());
    }

    @Test
    // TC-ACC-SER-007: Repository save throws RuntimeException during createAccount
    // Expected result: Transaction rollback - account NOT persisted
    void TC_ACC_SER_007() {
        // Arrange
        Account newAccount = new Account();
        newAccount.setUsername("newuser");
        newAccount.setEmail("new@test.com");
        newAccount.setPasswordHash("password123");

        when(accountRepository.existsById("newuser")).thenReturn(false);
        when(accountRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordSecurity.isPasswordHashed(anyString())).thenReturn(false);
        when(passwordSecurity.hashPassword(anyString())).thenReturn("$2a$12$hashedPassword");
        when(accountRepository.save(any())).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            accountService.createAccount(newAccount, null);
        });

        verify(accountRepository).save(any());
    }

    @Test
    // Test successful account creation
    // Expected result: Account saved with hashed password
    void testCreateAccountSuccess() {
        // Arrange
        Account newAccount = new Account();
        newAccount.setUsername("newuser");
        newAccount.setEmail("new@test.com");
        newAccount.setPasswordHash("plainPassword");

        when(accountRepository.existsById("newuser")).thenReturn(false);
        when(accountRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordSecurity.isPasswordHashed("plainPassword")).thenReturn(false);
        when(passwordSecurity.hashPassword("plainPassword")).thenReturn("$2a$12$hashedPassword");
        when(accountRepository.save(any())).thenReturn(newAccount);

        // Act
        Account result = accountService.createAccount(newAccount, null);

        // Assert
        assertNotNull(result);
        verify(accountRepository).existsById("newuser");
        verify(accountRepository).existsByEmail("new@test.com");
        verify(passwordSecurity).hashPassword("plainPassword");
        verify(accountRepository).save(any());
    }

    @Test
    // Test authenticate with valid credentials
    // Expected result: Return success AuthResult
    void testAuthenticateSuccess() {
        // Arrange
        when(accountRepository.findByUsernameOrEmailOrPhone("testuser")).thenReturn(Optional.of(testAccount));
        when(passwordSecurity.verifyPassword("password123", "$2a$12$hashedPassword")).thenReturn(true);

        // Act
        AuthResult result = accountService.authenticateWithResult("testuser", "password123");

        // Assert
        assertTrue(result.isSuccess());
        assertEquals(testAccount, result.account());
        verify(accountRepository).findByUsernameOrEmailOrPhone("testuser");
        verify(passwordSecurity).verifyPassword("password123", "$2a$12$hashedPassword");
    }

    @Test
    // Test authenticate with invalid password
    // Expected result: Return invalid credentials AuthResult
    void testAuthenticateInvalidPassword() {
        // Arrange
        when(accountRepository.findByUsernameOrEmailOrPhone("testuser")).thenReturn(Optional.of(testAccount));
        when(passwordSecurity.verifyPassword("wrongpassword", "$2a$12$hashedPassword")).thenReturn(false);

        // Act
        AuthResult result = accountService.authenticateWithResult("testuser", "wrongpassword");

        // Assert
        assertFalse(result.isSuccess());
        verify(accountRepository).findByUsernameOrEmailOrPhone("testuser");
        verify(passwordSecurity).verifyPassword("wrongpassword", "$2a$12$hashedPassword");
    }

    @Test
    // Test authenticate with not activated account
    // Expected result: Return not activated AuthResult
    void testAuthenticateNotActivated() {
        // Arrange
        testAccount.setActivated(false);
        when(accountRepository.findByUsernameOrEmailOrPhone("testuser")).thenReturn(Optional.of(testAccount));

        // Act
        AuthResult result = accountService.authenticateWithResult("testuser", "password123");

        // Assert
        assertFalse(result.isSuccess());
        verify(accountRepository).findByUsernameOrEmailOrPhone("testuser");
        verify(passwordSecurity, never()).verifyPassword(anyString(), anyString());
    }

    @Test
    // Test authenticate with non-existent user
    // Expected result: Return invalid credentials AuthResult (timing attack prevention)
    void testAuthenticateUserNotFound() {
        // Arrange
        when(accountRepository.findByUsernameOrEmailOrPhone("ghost_user")).thenReturn(Optional.empty());
        when(passwordSecurity.verifyPassword(anyString(), anyString())).thenReturn(false);

        // Act
        AuthResult result = accountService.authenticateWithResult("ghost_user", "password123");

        // Assert
        assertFalse(result.isSuccess());
        verify(accountRepository).findByUsernameOrEmailOrPhone("ghost_user");
        // Verify dummy BCrypt check was performed (timing attack prevention)
        verify(passwordSecurity).verifyPassword(eq("password123"), anyString());
    }

    @Test
    // Test toggle activation success
    // Expected result: Account activation status toggled
    void testToggleActivationSuccess() {
        // Arrange
        when(accountRepository.findById("testuser")).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any())).thenReturn(testAccount);

        // Act
        Account result = accountService.toggleActivation("testuser");

        // Assert
        assertNotNull(result);
        assertFalse(result.getActivated()); // Was true, now false
        verify(accountRepository).findById("testuser");
        verify(accountRepository).save(testAccount);
    }

    @Test
    // Test delete last admin
    // Expected result: Throw BusinessRuleException
    void testDeleteLastAdmin() {
        // Arrange
        testAccount.setAdmin(true);
        when(accountRepository.findById("testuser")).thenReturn(Optional.of(testAccount));
        when(accountRepository.countByAdminTrue()).thenReturn(1L);

        // Act & Assert
        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> {
            accountService.deleteOrThrow("testuser");
        });

        assertEquals("Không thể xóa admin cuối cùng trong hệ thống!", exception.getMessage());
        verify(accountRepository).findById("testuser");
        verify(accountRepository).countByAdminTrue();
        verify(accountRepository, never()).deleteById(anyString());
    }

    @Test
    // Test delete non-admin account success
    // Expected result: Account deleted
    void testDeleteAccountSuccess() {
        // Arrange
        when(accountRepository.findById("testuser")).thenReturn(Optional.of(testAccount));
        doNothing().when(accountRepository).deleteById("testuser");

        // Act
        accountService.deleteOrThrow("testuser");

        // Assert
        verify(accountRepository).findById("testuser");
        verify(accountRepository).deleteById("testuser");
    }

    @Test
    // Test admin reset password success
    // Expected result: Password updated with new hash
    void testAdminResetPasswordSuccess() {
        // Arrange
        when(accountRepository.findById("testuser")).thenReturn(Optional.of(testAccount));
        when(passwordSecurity.hashPassword("NewPass@123")).thenReturn("$2a$12$newHashedPassword");
        when(accountRepository.save(any())).thenReturn(testAccount);

        // Act
        accountService.adminResetPassword("testuser", "NewPass@123");

        // Assert
        verify(accountRepository).findById("testuser");
        verify(passwordSecurity).hashPassword("NewPass@123");
        verify(accountRepository).save(testAccount);
    }

    @Test
    // Test update phone success
    // Expected result: Phone number updated
    void testUpdatePhoneSuccess() {
        // Arrange
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any())).thenReturn(testAccount);

        // Act
        accountService.updatePhone("testuser", "0987654321");

        // Assert
        assertEquals("0987654321", testAccount.getPhone());
        verify(accountRepository).findByUsername("testuser");
        verify(accountRepository).save(testAccount);
    }

    @Test
    // Test find by email success
    // Expected result: Return account
    void testFindByEmailSuccess() {
        // Arrange
        when(accountRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testAccount));

        // Act
        Account result = accountService.findByEmail("test@example.com");

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(accountRepository).findByEmail("test@example.com");
    }

    @Test
    // Test find by email not found
    // Expected result: Return null
    void testFindByEmailNotFound() {
        // Arrange
        when(accountRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        // Act
        Account result = accountService.findByEmail("notfound@example.com");

        // Assert
        assertNull(result);
        verify(accountRepository).findByEmail("notfound@example.com");
    }

    @Test
    // Test reset password with valid token
    // Expected result: Password updated and token cleared
    void testResetPasswordSuccess() {
        // Arrange
        testAccount.setResetToken("valid_token");
        testAccount.setResetTokenExpiry(LocalDateTime.now().plusHours(1));

        when(accountRepository.findByResetToken("valid_token")).thenReturn(Optional.of(testAccount));
        when(passwordSecurity.hashPassword("NewPassword@123")).thenReturn("$2a$12$newHashedPassword");
        when(accountRepository.save(any())).thenReturn(testAccount);

        // Act
        accountService.resetPassword("valid_token", "NewPassword@123");

        // Assert
        assertNull(testAccount.getResetToken());
        assertNull(testAccount.getResetTokenExpiry());
        assertNotNull(testAccount.getLastPasswordResetDate());
        verify(accountRepository).findByResetToken("valid_token");
        verify(passwordSecurity).hashPassword("NewPassword@123");
        verify(accountRepository).save(testAccount);
    }

    @Test
    // Test reset password with expired token
    // Expected result: Throw ValidationException
    void testResetPasswordExpiredToken() {
        // Arrange
        testAccount.setResetToken("expired_token");
        testAccount.setResetTokenExpiry(LocalDateTime.now().minusHours(1));

        when(accountRepository.findByResetToken("expired_token")).thenReturn(Optional.of(testAccount));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            accountService.resetPassword("expired_token", "NewPassword@123");
        });

        assertEquals("Token đã hết hạn. Vui lòng yêu cầu đặt lại mật khẩu lại!", exception.getMessage());
        verify(accountRepository).findByResetToken("expired_token");
        verify(accountRepository, never()).save(any());
    }

    @Test
    // Test reset password with invalid token
    // Expected result: Throw ValidationException
    void testResetPasswordInvalidToken() {
        // Arrange
        when(accountRepository.findByResetToken("invalid_token")).thenReturn(Optional.empty());

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            accountService.resetPassword("invalid_token", "NewPassword@123");
        });

        assertEquals("Token đặt lại mật khẩu không hợp lệ!", exception.getMessage());
        verify(accountRepository).findByResetToken("invalid_token");
        verify(accountRepository, never()).save(any());
    }
}
