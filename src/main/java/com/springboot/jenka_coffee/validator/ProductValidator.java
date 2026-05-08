package com.springboot.jenka_coffee.validator;

import com.springboot.jenka_coffee.exception.BusinessRuleException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

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
        "image/jpeg", "image/jpg", "image/png", "image/webp"
    );
    
    /**
     * Validate product price
     */
    public void validatePrice(BigDecimal price) {
        if (price == null) {
            throw new BusinessRuleException("Giá sản phẩm không được để trống");
        }
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Giá sản phẩm không thể âm");
        }
    }
    
    /**
     * Validate product name
     */
    public void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessRuleException("Tên sản phẩm không được để trống");
        }
        if (name.length() > 255) {
            throw new BusinessRuleException("Tên sản phẩm không được vượt quá 255 ký tự");
        }
    }
    
    /**
     * Validate image file
     */
    public void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return; // Optional file
        }

        // TC-SEC-002 FIX: Validate filename for path traversal attack prevention
        // Attacker can send filename "../../etc/passwd.jpg" to traverse directories
        String filename = file.getOriginalFilename();
        if (filename != null) {
            // Check for path traversal sequences
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                throw new BusinessRuleException("Tên file không hợp lệ: chứa ký tự nguy hiểm (path traversal)");
            }
            // Additional: only allow safe characters in filename
            if (!filename.matches("^[a-zA-Z0-9._\\-\\s]+$")) {
                throw new BusinessRuleException("Tên file không hợp lệ: chỉ chấp nhận chữ cái, số, dấu chấm, gạch ngang");
            }
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessRuleException("Kích thước file không được vượt quá 5MB");
        }

        // Check file type (MIME type from request header)
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessRuleException("Chỉ chấp nhận file ảnh định dạng: JPG, PNG, WEBP");
        }
    }
    
    /**
     * Validate multiple image files
     */
    public void validateImageFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new BusinessRuleException("Vui lòng chọn ít nhất 1 ảnh");
        }
        
        if (files.size() > 10) {
            throw new BusinessRuleException("Chỉ được upload tối đa 10 ảnh");
        }
        
        // TC-PRD-CTRL-044 FIX: Check for duplicate filenames in same request
        java.util.Set<String> filenames = new java.util.HashSet<>();
        for (MultipartFile file : files) {
            String filename = file.getOriginalFilename();
            if (filename != null && !filenames.add(filename)) {
                throw new BusinessRuleException("File trùng lặp: " + filename + ". Vui lòng chọn các file khác nhau.");
            }
        }
        
        for (MultipartFile file : files) {
            validateImageFile(file);
        }
    }
}
