package com.springboot.jenka_coffee.util;

import org.springframework.stereotype.Component;

/**
 * Image utility helper with fallback support
 * Provides default images when cloud images are not available
 */
@Component
public class ImageHelper {

    private static final String DEFAULT_PRODUCT_IMAGE = "/images/icon_category/ca_phe_do_an.webp";
    private static final String DEFAULT_CATEGORY_ICON = "/images/icon_category/ca_phe_do_an.webp";
    private static final String DEFAULT_AVATAR = "/images/default-avatar.png";

    /**
     * Get product image URL with fallback
     * 
     * @param imageName    Image filename from database (can be null or empty)
     * @param cloudBaseUrl Base URL for cloud storage (can be null)
     * @return Full image URL or default image path
     */
    public String getProductImage(String imageName, String cloudBaseUrl) {
        if (imageName == null || imageName.trim().isEmpty()) {
            return DEFAULT_PRODUCT_IMAGE;
        }

        // If cloud URL is configured, use it
        if (cloudBaseUrl != null && !cloudBaseUrl.trim().isEmpty()) {
            return cloudBaseUrl + "/" + imageName;
        }

        // Otherwise try local uploads folder
        return "/uploads/" + imageName;
    }

    /**
     * Get product image with default fallback
     */
    public String getProductImage(String imageName) {
        return getProductImage(imageName, null);
    }

    /**
     * Get category icon with fallback
     */
    public String getCategoryIcon(String iconName) {
        if (iconName == null || iconName.trim().isEmpty()) {
            return DEFAULT_CATEGORY_ICON;
        }
        return "/images/icon_category/" + iconName;
    }

    /**
     * Get user avatar with fallback
     */
    public String getUserAvatar(String avatarName) {
        if (avatarName == null || avatarName.trim().isEmpty()) {
            return DEFAULT_AVATAR;
        }
        return "/uploads/" + avatarName;
    }

    /**
     * Get default product image
     */
    public String getDefaultProductImage() {
        return DEFAULT_PRODUCT_IMAGE;
    }
}
