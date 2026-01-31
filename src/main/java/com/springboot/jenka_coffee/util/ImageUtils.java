package com.springboot.jenka_coffee.util;

import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * Utility class for image processing operations
 */
public class ImageUtils {

    // Supported image formats
    private static final List<String> SUPPORTED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    // File extensions
    private static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp"
    );

    // Max file size (5MB)
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    /**
     * Validate if uploaded file is a valid image
     */
    public static boolean isValidImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            return false;
        }

        // Check content type
        String contentType = file.getContentType();
        if (contentType == null || !SUPPORTED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            return false;
        }

        // Check file extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return false;
        }

        String extension = getFileExtension(originalFilename);
        return SUPPORTED_EXTENSIONS.contains(extension.toLowerCase());
    }

    /**
     * Get file extension from filename
     */
    public static String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    /**
     * Generate safe filename for upload
     */
    public static String generateSafeFilename(String originalFilename) {
        if (originalFilename == null) {
            return "image_" + System.currentTimeMillis() + ".jpg";
        }

        String extension = getFileExtension(originalFilename);
        String baseName = originalFilename.substring(0, originalFilename.lastIndexOf("."));
        
        // Remove special characters and spaces
        baseName = baseName.replaceAll("[^a-zA-Z0-9]", "_");
        
        // Limit length
        if (baseName.length() > 50) {
            baseName = baseName.substring(0, 50);
        }

        return baseName + "_" + System.currentTimeMillis() + "." + extension;
    }

    /**
     * Get validation error message for invalid image
     */
    public static String getImageValidationError(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "Vui lòng chọn file ảnh";
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return "Kích thước file không được vượt quá 5MB";
        }

        String contentType = file.getContentType();
        if (contentType == null || !SUPPORTED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            return "Định dạng file không được hỗ trợ. Chỉ chấp nhận: JPG, PNG, GIF, WebP";
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return "Tên file không hợp lệ";
        }

        String extension = getFileExtension(originalFilename);
        if (!SUPPORTED_EXTENSIONS.contains(extension.toLowerCase())) {
            return "Phần mở rộng file không được hỗ trợ";
        }

        return null; // No error
    }

    /**
     * Image processing presets
     */
    public static class ImagePresets {
        public static final int PRODUCT_WIDTH = 800;
        public static final float PRODUCT_QUALITY = 0.85f;

        public static final int AVATAR_WIDTH = 400;
        public static final float AVATAR_QUALITY = 0.8f;

        public static final int NEWS_WIDTH = 1200;
        public static final float NEWS_QUALITY = 0.85f;

        public static final int THUMBNAIL_WIDTH = 300;
        public static final float THUMBNAIL_QUALITY = 0.75f;
    }
}