package com.springboot.jenka_coffee.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface UploadService {
    /**
     * Save image to cloud storage (Cloudinary)
     * 
     * @param file Image file to upload
     * @return URL of uploaded image, or null if failed
     */
    String saveImage(MultipartFile file);

    /**
     * Save and compress product image before uploading
     * 
     * @param file Product image file
     * @return URL of uploaded image, or null if failed
     */
    String saveProductImage(MultipartFile file);

    /**
     * Save and compress news image before uploading
     * 
     * @param file News image file  
     * @return URL of uploaded image, or null if failed
     */
    String saveNewsImage(MultipartFile file);

    /**
     * Save image with custom compression settings
     * 
     * @param file Image file
     * @param targetWidth Target width for resizing
     * @param quality Compression quality (0.1-1.0)
     * @return URL of uploaded image, or null if failed
     */
    String saveImageWithCompression(MultipartFile file, int targetWidth, float quality);


    String uploadFile(MultipartFile file, String subfolder) throws IOException;

    String getUploadDir();

    /**
     * VULN-ORPHANED-STORAGE FIX: Delete image from cloud storage (Cloudinary)
     * Prevents storage exhaustion from orphaned files
     * 
     * @param imageUrl Full URL of the image to delete
     */
    void deleteImage(String imageUrl);
}