package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.dto.request.ProfileUpdateRequest;
import com.springboot.jenka_coffee.entity.Account;
import org.springframework.web.multipart.MultipartFile;

public interface ProfileService {
    
    /**
     * Get user profile by username
     * 
     * @param username Username
     * @return Account profile
     */
    Account getProfile(String username);
    
    /**
     * Update user profile information
     * 
     * @param username Username
     * @param request Profile update request
     * @return Updated account
     */
    Account updateProfile(String username, ProfileUpdateRequest request);
    
    /**
     * Update user avatar with image compression
     * 
     * @param username Username
     * @param avatarFile Avatar image file
     * @return Updated account with new avatar path
     */
    Account updateAvatar(String username, MultipartFile avatarFile);

    /**
     * Change user password
     *
     * @param username        Username
     * @param currentPassword Current password for verification
     * @param newPassword     New password
     */
    void changePassword(String username, String currentPassword, String newPassword);

    /**
     * Validate profile update request
     * 
     * @param request Profile update request
     * @param currentAccount Current account data
     */
    void validateProfileUpdate(ProfileUpdateRequest request, Account currentAccount);
}