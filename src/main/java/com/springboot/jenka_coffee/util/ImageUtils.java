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
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp");

    // File extensions
    private static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp");

    // Max file size (5MB)
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    /**
     * Validate if uploaded file is a valid image.
     * VULN-041 FIX: Thêm magic bytes check — không tin Content-Type header từ client.
     */
    public static boolean isValidImage(MultipartFile file) {
        if (file == null || file.isEmpty()) return false;
        if (file.getSize() > MAX_FILE_SIZE) return false;

        // Check content type (client-controlled, but first filter)
        String contentType = file.getContentType();
        if (contentType == null || !SUPPORTED_IMAGE_TYPES.contains(contentType.toLowerCase())) return false;

        // Check file extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) return false;
        String extension = getFileExtension(originalFilename);
        if (!SUPPORTED_EXTENSIONS.contains(extension.toLowerCase())) return false;

        // VULN-041 FIX: Magic bytes validation — verify actual file format
        try {
            byte[] header = new byte[12];
            int read = file.getInputStream().read(header);
            if (read < 4) return false;
            return isValidImageMagicBytes(header);
        } catch (java.io.IOException e) {
            return false;
        }
    }

    /**
     * Verify file magic bytes — cannot be spoofed by Content-Type header.
     */
    private static boolean isValidImageMagicBytes(byte[] header) {
        // JPEG: FF D8 FF
        if (header[0] == (byte)0xFF && header[1] == (byte)0xD8 && header[2] == (byte)0xFF)
            return true;
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (header[0] == (byte)0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47)
            return true;
        // GIF: 47 49 46 38 (GIF8)
        if (header[0] == 0x47 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x38)
            return true;
        // WebP: 52 49 46 46 ?? ?? ?? ?? 57 45 42 50 (RIFF....WEBP)
        return header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46
                && header.length >= 12 && header[8] == 0x57 && header[9] == 0x45
                && header[10] == 0x42 && header[11] == 0x50;
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
            return "Kích thước file quá lớn. Vui lòng chọn file dưới 5MB.";
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

        public static final int AVATAR_WIDTH = 400;
        public static final float AVATAR_QUALITY = 0.8f;

    }
}