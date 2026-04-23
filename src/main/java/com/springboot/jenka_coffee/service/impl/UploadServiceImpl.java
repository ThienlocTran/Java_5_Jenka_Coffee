package com.springboot.jenka_coffee.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.springboot.jenka_coffee.exception.InvalidFileException;
import com.springboot.jenka_coffee.service.UploadService;
import com.springboot.jenka_coffee.util.ImageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class UploadServiceImpl implements UploadService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private final Cloudinary cloudinary;
    private final String uploadDir;

    public UploadServiceImpl(Cloudinary cloudinary,
                             @Value("${app.upload.directory:uploads}") String uploadDir) {
        this.cloudinary = cloudinary;
        this.uploadDir = uploadDir;

        // Create upload directory if not exists
        File uploadDirectory = new File(uploadDir);
        if (!uploadDirectory.exists()) {
            boolean created = uploadDirectory.mkdirs();
            if (!created) {
                log.warn("Failed to create upload directory: {}", uploadDir);
            }
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
     * NOTE: This method returns the secure_url from Cloudinary.
     * The public_id is embedded in the URL and can be extracted for deletion.
     * Format: <a href="https://res.cloudinary.com/">...</a>{cloud_name}/image/upload/v{version}/{public_id}.{format}
     * @param file the file to upload
     * @return the secure URL of uploaded image
     * @throws RuntimeException if upload fails
     */
    private String uploadToCloudinary(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("Attempted to upload null or empty file");
            throw new RuntimeException("File is null or empty");
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("File size exceeds maximum allowed size of " + (MAX_FILE_SIZE / 1024 / 1024) + "MB");
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

            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(tempFile, ObjectUtils.asMap(
                    "resource_type", "auto",
                    "folder", "jenka_coffee",
                    "use_filename", true,
                    "unique_filename", true,
                    "quality", "auto:good",
                    "fetch_format", "auto"
            ));

            String secureUrl = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");
            
            log.info("Uploaded: {} -> {} (public_id: {})", file.getOriginalFilename(), secureUrl, publicId);
            return secureUrl;

        } catch (InvalidFileException e) {
            throw e;
        } catch (IOException e) {
            log.error("IO error uploading image: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Failed to upload image: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error uploading image: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Failed to upload image: " + e.getMessage(), e);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                try {
                    Files.deleteIfExists(tempFile.toPath());
                } catch (IOException e) {
                    log.warn("Failed to delete temp file: {}", tempFile.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Create a temporary file from MultipartFile
     * @param file the multipart file to convert
     * @return a temporary File object
     * @throws IOException if file creation fails
     */
    private File createTempFile(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String safeFilename = ImageUtils.generateSafeFilename(originalFilename);
        String extension = getExtension(safeFilename);

        // Create temp file with safe name
        File tempFile = File.createTempFile("upload_" + UUID.randomUUID(), "." + extension);

        try (java.io.InputStream is = file.getInputStream()) {
            Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        return tempFile;
    }

    /**
     * Extract file extension from filename
     * @param filename the filename
     * @return the extension without dot
     */
    private String getExtension(String filename) {
        return ImageUtils.getFileExtension(filename);
    }


    @Override
    public String uploadFile(MultipartFile file, String subfolder) throws IOException {
        final String extension = getString(file, subfolder);

        // VULN-042 FIX: Canonical path check — ngăn path traversal
        File baseDir = new File(uploadDir).getCanonicalFile();
        File subfolderDir = new File(baseDir, subfolder).getCanonicalFile();
        if (!subfolderDir.getAbsolutePath().startsWith(baseDir.getAbsolutePath())) {
            throw new IOException("Path traversal detected!");
        }

        if (!subfolderDir.exists()) {
            boolean created = subfolderDir.mkdirs();
            if (!created) {
                throw new IOException("Failed to create directory: " + subfolderDir.getAbsolutePath());
            }
        }

        String uniqueFilename = UUID.randomUUID() + extension;
        File targetFile = new File(subfolderDir, uniqueFilename).getCanonicalFile();

        // Final path traversal check on target file
        if (!targetFile.getAbsolutePath().startsWith(subfolderDir.getAbsolutePath())) {
            throw new IOException("Path traversal detected in filename!");
        }

        try (InputStream is = file.getInputStream()) {
            Files.copy(is, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        String relativePath = subfolder + "/" + uniqueFilename;
        log.info("Uploaded file to local storage: {}", relativePath);
        return relativePath;
    }

    private static String getString(MultipartFile file, String subfolder) throws IOException {
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
        List<String> ALLOWED_EXT = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp");
        if (!ALLOWED_EXT.contains(extension)) {
            throw new IOException("File extension not allowed: " + extension);
        }
        return extension;
    }

    @Override
    public String getUploadDir() {
        return uploadDir;
    }
    
    /**
     * VULN-ORPHANED-STORAGE FIX: Delete an image from Cloudinary
     * Extracts public_id from URL and calls Cloudinary destroy API
     * IMPORTANT: This method uses URL parsing to extract public_id.
     * This is fragile and depends on Cloudinary URL format remaining stable.
     *BETTER APPROACH (for future refactoring):
     * - Store public_id in database alongside the URL when uploading
     * - Pass public_id directly to this method instead of parsing URL
     * - This eliminates parsing fragility and makes deletion more reliable
     *
     * @param imageUrl the Cloudinary image URL (e.g., <a href="https://res.cloudinary.com/xxx/image/upload/v123/jenka_coffee/abc.jpg">...</a>)
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
            String publicId = extractPublicIdFromUrl(imageUrl);
            if (publicId == null) {
                log.warn("Could not extract public_id from URL: {}", imageUrl);
                return;
            }
            
            String status = destroyCloudinaryImage(publicId);
            
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
     * Call Cloudinary API to destroy an image
     * @param publicId the Cloudinary public_id
     * @return the status from Cloudinary API
     * @throws Exception if API call fails
     */
    private String destroyCloudinaryImage(String publicId) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        return (String) result.get("result");
    }
    
    /**
     * Extract public_id from Cloudinary URL
     * WARNING: This parsing is fragile and depends on Cloudinary URL format.
     * If Cloudinary changes their URL structure, this will break silently.
     * Example: <a href="https://res.cloudinary.com/xxx/image/upload/v123/jenka_coffee/abc.jpg">...</a> -> jenka_coffee/abc
     * RECOMMENDED: Store public_id in database during upload instead of parsing URL.
     */
    private String extractPublicIdFromUrl(String url) {
        try {
            // Find "/upload/" in URL
            int uploadIndex = url.indexOf("/upload/");
            if (uploadIndex == -1) {
                return null;
            }
            
            // Get everything after "/upload/vXXX/"
            final String afterUpload = getString(url, uploadIndex);

            return afterUpload;
        } catch (Exception e) {
            log.error("Error extracting public_id from URL: {}", url, e);
            return null;
        }
    }

    private static String getString(String url, int uploadIndex) {
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
    }
}