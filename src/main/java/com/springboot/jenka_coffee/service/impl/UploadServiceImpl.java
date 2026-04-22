package com.springboot.jenka_coffee.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.springboot.jenka_coffee.exception.InvalidFileException;
import com.springboot.jenka_coffee.service.UploadService;
import com.springboot.jenka_coffee.util.ImageUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class UploadServiceImpl implements UploadService {

    private final Cloudinary cloudinary;
    private final String uploadDir = "uploads";

    public UploadServiceImpl(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;

        // Create upload directory if not exists
        File uploadDirectory = new File(uploadDir);
        if (!uploadDirectory.exists()) {
            uploadDirectory.mkdirs();
        }
    }

    @Override
    public String saveImage(MultipartFile file) {
        return uploadToCloudinary(file);
    }

    @Override
    public String saveProductImage(MultipartFile file) {
        return uploadToCloudinary(file);
    }

    @Override
    public String saveNewsImage(MultipartFile file) {
        return uploadToCloudinary(file);
    }

    @Override
    public String saveImageWithCompression(MultipartFile file, int targetWidth, float quality) {
        // targetWidth/quality ignored — Cloudinary handles optimization server-side
        return uploadToCloudinary(file);
    }

    /**
     * Upload directly to Cloudinary without any local processing.
     * Cloudinary handles resize/compression via quality:auto and fetch_format:auto.
     */
    @SneakyThrows
    private String uploadToCloudinary(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("Attempted to upload null or empty file");
            return null;
        }

        // Validate image file
        if (!ImageUtils.isValidImage(file)) {
            String error = ImageUtils.getImageValidationError(file);
            log.error("Invalid image file: {}", error);
            throw new InvalidFileException(error);
        }

        File tempFile = null;
        try {
            tempFile = createTempFile(file);

            Map uploadResult = cloudinary.uploader().upload(tempFile, ObjectUtils.asMap(
                    "resource_type", "auto",
                    "folder", "jenka_coffee",
                    "use_filename", true,
                    "unique_filename", true,
                    "quality", "auto:good",
                    "fetch_format", "auto"
            ));

            String secureUrl = (String) uploadResult.get("secure_url");
            log.info("Uploaded: {} -> {}", file.getOriginalFilename(), secureUrl);
            return secureUrl;

        } catch (Exception e) {
            log.error("Failed to upload image: {}", file.getOriginalFilename(), e);
            if (e instanceof InvalidFileException) throw e;
            return null;
        } finally {
            if (tempFile != null && tempFile.exists()) {
                try {
                    Files.delete(tempFile.toPath());
                } catch (IOException e) {
                    log.warn("Failed to delete temp file: {}", tempFile.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Create temporary file from MultipartFile
     */
    private File createTempFile(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String safeFilename = ImageUtils.generateSafeFilename(originalFilename);
        String extension = ImageUtils.getFileExtension(safeFilename);

        // Create temp file with safe name
        File tempFile = File.createTempFile("upload_" + UUID.randomUUID(), "." + extension);

        try (java.io.InputStream is = file.getInputStream()) {
            Files.copy(is, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        return tempFile;
    }


    @Override
    public String uploadFile(MultipartFile file, String subfolder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("File is null or empty");
        }

        // VULN-042 FIX: Whitelist subfolder — chỉ cho phép alphanumeric + underscore/dash
        if (subfolder == null || !subfolder.matches("^[a-zA-Z0-9_-]+$")) {
            throw new IOException("Invalid subfolder name: " + subfolder);
        }

        // VULN-042 FIX: Whitelist extension — chỉ cho phép ảnh
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }
        java.util.List<String> ALLOWED_EXT = java.util.Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp");
        if (!ALLOWED_EXT.contains(extension)) {
            throw new IOException("File extension not allowed: " + extension);
        }

        // VULN-042 FIX: Canonical path check — ngăn path traversal
        File baseDir = new File(uploadDir).getCanonicalFile();
        File subfolderDir = new File(baseDir, subfolder).getCanonicalFile();
        if (!subfolderDir.getAbsolutePath().startsWith(baseDir.getAbsolutePath())) {
            throw new IOException("Path traversal detected!");
        }

        if (!subfolderDir.exists()) subfolderDir.mkdirs();

        String uniqueFilename = UUID.randomUUID().toString() + extension;
        File targetFile = new File(subfolderDir, uniqueFilename).getCanonicalFile();

        // Final path traversal check on target file
        if (!targetFile.getAbsolutePath().startsWith(subfolderDir.getAbsolutePath())) {
            throw new IOException("Path traversal detected in filename!");
        }

        try (InputStream is = file.getInputStream()) {
            Files.copy(is, targetFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        String relativePath = subfolder + "/" + uniqueFilename;
        log.info("Uploaded file to local storage: {}", relativePath);
        return relativePath;
    }

    @Override
    public String getUploadDir() {
        return uploadDir;
    }
    
    /**
     * VULN-ORPHANED-STORAGE FIX: Delete image from Cloudinary
     * Extracts public_id from URL and calls Cloudinary destroy API
     */
    @Override
    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }
        
        // Only delete Cloudinary images (not external URLs like ui-avatars.com)
        if (!imageUrl.contains("cloudinary.com")) {
            log.debug("Skipping deletion of non-Cloudinary URL: {}", imageUrl);
            return;
        }
        
        try {
            // Extract public_id from Cloudinary URL
            // Example: https://res.cloudinary.com/xxx/image/upload/v123/jenka_coffee/abc.jpg
            // public_id = jenka_coffee/abc
            String publicId = extractPublicIdFromUrl(imageUrl);
            if (publicId == null) {
                log.warn("Could not extract public_id from URL: {}", imageUrl);
                return;
            }
            
            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            String status = (String) result.get("result");
            
            if ("ok".equals(status)) {
                log.info("Successfully deleted image from Cloudinary: {}", publicId);
            } else {
                log.warn("Failed to delete image from Cloudinary: {} - Status: {}", publicId, status);
            }
        } catch (Exception e) {
            log.error("Error deleting image from Cloudinary: {}", imageUrl, e);
            // Don't throw - deletion failure shouldn't block the main operation
        }
    }
    
    /**
     * Extract public_id from Cloudinary URL
     * Example: https://res.cloudinary.com/xxx/image/upload/v123/jenka_coffee/abc.jpg -> jenka_coffee/abc
     */
    private String extractPublicIdFromUrl(String url) {
        try {
            // Find "/upload/" in URL
            int uploadIndex = url.indexOf("/upload/");
            if (uploadIndex == -1) {
                return null;
            }
            
            // Get everything after "/upload/vXXX/"
            String afterUpload = url.substring(uploadIndex + 8); // 8 = length of "/upload/"
            
            // Skip version number if present (e.g., "v1234567890/")
            if (afterUpload.startsWith("v") && afterUpload.contains("/")) {
                afterUpload = afterUpload.substring(afterUpload.indexOf("/") + 1);
            }
            
            // Remove file extension
            int lastDot = afterUpload.lastIndexOf(".");
            if (lastDot > 0) {
                afterUpload = afterUpload.substring(0, lastDot);
            }
            
            return afterUpload;
        } catch (Exception e) {
            log.error("Error extracting public_id from URL: {}", url, e);
            return null;
        }
    }
}