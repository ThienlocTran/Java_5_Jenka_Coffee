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
import java.io.FileOutputStream;
import java.io.IOException;
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

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(file.getBytes());
        }

        return tempFile;
    }


    @Override
    public String uploadFile(MultipartFile file, String subfolder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("File is null or empty");
        }

        // Create subfolder if not exists
        File subfolderDir = new File(uploadDir, subfolder);
        if (!subfolderDir.exists()) {
            subfolderDir.mkdirs();
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String uniqueFilename = UUID.randomUUID().toString() + extension;
        File targetFile = new File(subfolderDir, uniqueFilename);

        // Save file
        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
            fos.write(file.getBytes());
        }

        // Return relative path
        String relativePath = subfolder + "/" + uniqueFilename;
        log.info("Uploaded file to local storage: {}", relativePath);
        return relativePath;
    }

    @Override
    public String getUploadDir() {
        return uploadDir;
    }
}