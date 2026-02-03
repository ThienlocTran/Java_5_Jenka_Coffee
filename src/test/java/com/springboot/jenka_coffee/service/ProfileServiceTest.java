package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.dto.request.ProfileUpdateRequest;
import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.exception.ResourceNotFoundException;
import com.springboot.jenka_coffee.exception.ValidationException;
import com.springboot.jenka_coffee.repository.AccountRepository;
import com.springboot.jenka_coffee.service.impl.ProfileServiceImpl;
import com.springboot.jenka_coffee.util.PasswordSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UploadService uploadService;

    @Mock
    private ImageService imageService;

    @Mock
    private PasswordSecurity passwordSecurity;

    @InjectMocks
    private ProfileServiceImpl profileService;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setUsername("testuser");
        testAccount.setFullname("Test User");
        testAccount.setEmail("test@example.com");
        testAccount.setPhone("0123456789");
        testAccount.setPasswordHash("hashedPassword");
        testAccount.setActivated(true);
        testAccount.setAdmin(false);
        testAccount.setPoints(100);
        testAccount.setCustomerRank("MEMBER");
    }

    @Test
    void getProfile_ShouldReturnAccount_WhenAccountExists() {
        // Given
        when(accountRepository.findById("testuser")).thenReturn(Optional.of(testAccount));

        // When
        Account result = profileService.getProfile("testuser");

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("Test User", result.getFullname());
        verify(accountRepository).findById("testuser");
    }

    @Test
    void getProfile_ShouldThrowException_WhenAccountNotExists() {
        // Given
        when(accountRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            profileService.getProfile("nonexistent");
        });
    }

    @Test
    void updateProfile_ShouldUpdateBasicInfo_WhenValidRequest() {
        // Given
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setFullname("Updated Name");
        request.setEmail("updated@example.com");
        request.setPhone("0987654321");

        when(accountRepository.findById("testuser")).thenReturn(Optional.of(testAccount));
        when(accountRepository.existsByEmail("updated@example.com")).thenReturn(false);
        when(accountRepository.existsByPhone("0987654321")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // When
        Account result = profileService.updateProfile("testuser", request);

        // Then
        assertNotNull(result);
        assertEquals("Updated Name", testAccount.getFullname());
        assertEquals("updated@example.com", testAccount.getEmail());
        assertEquals("0987654321", testAccount.getPhone());
        verify(accountRepository).save(testAccount);
    }

    @Test
    void updateProfile_ShouldChangePassword_WhenPasswordProvided() {
        // Given
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setFullname("Test User");
        request.setCurrentPassword("currentPassword");
        request.setNewPassword("newPassword");
        request.setConfirmPassword("newPassword");

        when(accountRepository.findById("testuser")).thenReturn(Optional.of(testAccount));
        when(passwordSecurity.verifyPassword("currentPassword", "hashedPassword")).thenReturn(true);
        when(passwordSecurity.hashPassword("newPassword")).thenReturn("newHashedPassword");
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // When
        Account result = profileService.updateProfile("testuser", request);

        // Then
        assertNotNull(result);
        assertEquals("newHashedPassword", testAccount.getPasswordHash());
        verify(passwordSecurity, times(2)).verifyPassword("currentPassword", "hashedPassword"); // Called in validation and changePassword
        verify(passwordSecurity).hashPassword("newPassword");
        verify(accountRepository).save(testAccount);
    }

    @Test
    void validateProfileUpdate_ShouldThrowException_WhenEmailExists() {
        // Given
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setEmail("existing@example.com");

        when(accountRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // When & Then
        assertThrows(ValidationException.class, () -> {
            profileService.validateProfileUpdate(request, testAccount);
        });
    }

    @Test
    void validateProfileUpdate_ShouldThrowException_WhenPhoneExists() {
        // Given
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setPhone("0111111111");

        when(accountRepository.existsByPhone("0111111111")).thenReturn(true);

        // When & Then
        assertThrows(ValidationException.class, () -> {
            profileService.validateProfileUpdate(request, testAccount);
        });
    }

    @Test
    void validateProfileUpdate_ShouldThrowException_WhenPasswordsDontMatch() {
        // Given
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setCurrentPassword("currentPassword");
        request.setNewPassword("newPassword");
        request.setConfirmPassword("differentPassword");

        // When & Then
        assertThrows(ValidationException.class, () -> {
            profileService.validateProfileUpdate(request, testAccount);
        });
    }

    @Test
    void changePassword_ShouldUpdatePassword_WhenValidCredentials() {
        // Given
        when(accountRepository.findById("testuser")).thenReturn(Optional.of(testAccount));
        when(passwordSecurity.verifyPassword("currentPassword", "hashedPassword")).thenReturn(true);
        when(passwordSecurity.hashPassword("newPassword")).thenReturn("newHashedPassword");
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // When
        Account result = profileService.changePassword("testuser", "currentPassword", "newPassword");

        // Then
        assertNotNull(result);
        assertEquals("newHashedPassword", testAccount.getPasswordHash());
        verify(passwordSecurity).verifyPassword("currentPassword", "hashedPassword");
        verify(passwordSecurity).hashPassword("newPassword");
        verify(accountRepository).save(testAccount);
    }

    @Test
    void changePassword_ShouldThrowException_WhenCurrentPasswordWrong() {
        // Given
        when(accountRepository.findById("testuser")).thenReturn(Optional.of(testAccount));
        when(passwordSecurity.verifyPassword("wrongPassword", "hashedPassword")).thenReturn(false);

        // When & Then
        assertThrows(ValidationException.class, () -> {
            profileService.changePassword("testuser", "wrongPassword", "newPassword");
        });
    }
}