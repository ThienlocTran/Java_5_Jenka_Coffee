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
        
        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessRuleException("Kích thước file không được vượt quá 5MB");
        }
        
        // Check file type
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
        
        for (MultipartFile file : files) {
            validateImageFile(file);
        }
    }
}
