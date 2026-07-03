package com.springboot.jenka_coffee.validator;

import com.springboot.jenka_coffee.exception.BusinessRuleException;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Validator for Product business rules
 * Following Single Responsibility Principle
 */
@Component
public class ProductValidator {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_IMAGE_TYPES = List.of(
        "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"
    );
    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = List.of(
        "jpg", "jpeg", "png", "webp", "gif"
    );

    // TC-PRD-CTRL-046 FIX: Apache Tika for magic byte detection
    // Prevents fake image RCE attack where attacker sends PE binary with MIME type "image/jpeg"
    private static final Tika tika = new Tika();

    /**
     * Validate product price
     */
    public void validatePrice(BigDecimal price) {
        if (price == null) {
            return;
        }
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Gia san pham phai lon hon 0");
        }
    }

    /**
     * Validate product name
     */
    public void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessRuleException("Ten san pham khong duoc de trong");
        }
        if (name.length() > 255) {
            throw new BusinessRuleException("Ten san pham khong duoc vuot qua 255 ky tu");
        }
    }

    /**
     * Validate image file
     * TC-PRD-CTRL-046 FIX: Added Apache Tika magic byte detection to prevent fake image RCE
     */
    public void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return; // Optional file
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new BusinessRuleException("Ten file khong hop le");
        }

        // Only block traversal-style names; normal spaces, accents, plus signs, underscores are allowed.
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new BusinessRuleException("Ten file khong hop le: chua ky tu nguy hiem (path traversal)");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessRuleException("Kich thuoc file khong duoc vuot qua 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessRuleException("Chi chap nhan file anh dinh dang: JPG, JPEG, PNG, WEBP, GIF");
        }

        String extension = getFileExtension(filename);
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new BusinessRuleException("Phan mo rong file khong duoc ho tro. Chi chap nhan: JPG, JPEG, PNG, WEBP, GIF");
        }

        try {
            String detectedType = tika.detect(file.getInputStream());

            String normalizedType = detectedType.toLowerCase();
            if (normalizedType.equals("image/jpg")) {
                normalizedType = "image/jpeg";
            }

            if (!ALLOWED_IMAGE_TYPES.contains(normalizedType)) {
                throw new BusinessRuleException(
                    "File khong phai anh hop le. Phat hien loai file: " + detectedType +
                    ". Chi chap nhan: JPG, JPEG, PNG, WEBP, GIF"
                );
            }
        } catch (IOException e) {
            throw new BusinessRuleException("Khong the doc file de kiem tra. Vui long thu lai.");
        }
    }

    /**
     * Validate multiple image files
     */
    public void validateImageFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new BusinessRuleException("Vui long chon it nhat 1 anh");
        }

        if (files.size() > 10) {
            throw new BusinessRuleException("Chi duoc upload toi da 10 anh");
        }

        java.util.Set<String> filenames = new java.util.HashSet<>();
        for (MultipartFile file : files) {
            String filename = file.getOriginalFilename();
            if (filename != null && !filenames.add(filename)) {
                throw new BusinessRuleException("File trung lap: " + filename + ". Vui long chon cac file khac nhau.");
            }
        }

        for (MultipartFile file : files) {
            validateImageFile(file);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
