package com.springboot.jenka_coffee.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.springboot.jenka_coffee.exception.InvalidFileException;
import com.springboot.jenka_coffee.service.ImageService;
import com.springboot.jenka_coffee.service.UploadService;
import com.springboot.jenka_coffee.util.ImageUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class UploadServiceImpl implements UploadService {

    private final Cloudinary cloudinary;
    private final ImageService imageService;

    public UploadServiceImpl(Cloudinary cloudinary, ImageService imageService) {
        this.cloudinary = cloudinary;
        this.imageService = imageService;
    }

    @Override
    public String saveImage(MultipartFile file) {
        return uploadToCloudinary(file, false, 0, 0.0f);
    }

    @Override
    public String saveProductImage(MultipartFile file) {
        return uploadToCloudinary(file, true, ImageUtils.ImagePresets.PRODUCT_WIDTH, ImageUtils.ImagePresets.PRODUCT_QUALITY);
    }

    @Override
    public String saveNewsImage(MultipartFile file) {
        return uploadToCloudinary(file, true, ImageUtils.ImagePresets.NEWS_WIDTH, ImageUtils.ImagePresets.NEWS_QUALITY);
    }

    @Override
    public String saveImageWithCompression(MultipartFile file, int targetWidth, float quality) {
        return uploadToCloudinary(file, true, targetWidth, quality);
    }

    /**
     * Internal method to handle upload with optional compression
     */
    @SneakyThrows
    private String uploadToCloudinary(MultipartFile file, boolean compress, int targetWidth, float quality) {
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
            // Create temporary file
            tempFile = createTempFile(file);
            
            // Compress if requested and file is image
            if (compress && isImageFile(file)) {
                log.info("Compressing image before upload: {} ({}x{}, quality: {})", 
                        file.getOriginalFilename(), targetWidth, quality);
                imageService.processImage(tempFile, targetWidth, quality);
            }

            // Upload to Cloudinary
            Map uploadResult = cloudinary.uploader().upload(tempFile, ObjectUtils.asMap(
                    "resource_type", "auto",
                    "folder", "jenka_coffee", // Organize uploads in folder
                    "use_filename", true,
                    "unique_filename", true,
                    "transformation", ObjectUtils.asMap(
                            "quality", "auto:good", // Cloudinary auto quality optimization
                            "fetch_format", "auto"  // Auto format selection (WebP when supported)
                    )
            ));

            String secureUrl = (String) uploadResult.get("secure_url");
            log.info("Successfully uploaded image: {} -> {}", file.getOriginalFilename(), secureUrl);
            return secureUrl;

        } catch (Exception e) {
            log.error("Failed to upload image: {}", file.getOriginalFilename(), e);
            if (e instanceof InvalidFileException) {
                throw e; // Re-throw validation errors
            }
            return null;
        } finally {
            // Clean up temporary file
            if (tempFile != null && tempFile.exists()) {
                try {
                    Files.delete(tempFile.toPath());
                } catch (IOException e) {
                    log.warn("Failed to delete temporary file: {}", tempFile.getAbsolutePath(), e);
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
        
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(file.getBytes());
        }
        
        return tempFile;
    }

    /**
     * Check if file is an image based on content type
     */
    private boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }
}