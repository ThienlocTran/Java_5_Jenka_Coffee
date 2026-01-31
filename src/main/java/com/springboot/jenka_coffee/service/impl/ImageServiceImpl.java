package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.service.ImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

@Slf4j
@Service
public class ImageServiceImpl implements ImageService {

    // Default settings for different use cases
    private static final int DEFAULT_PRODUCT_WIDTH = 800;
    private static final int DEFAULT_NEWS_WIDTH = 1200;
    private static final float DEFAULT_QUALITY = 0.85f;

    @Override
    public void processImage(String filePath, int targetWidth, float quality) throws IOException {
        processImage(new File(filePath), targetWidth, quality);
    }

    @Override
    public void processImage(File file, int targetWidth, float quality) throws IOException {
        if (!file.exists()) {
            throw new IOException("File not found: " + file.getAbsolutePath());
        }

        log.info("Processing image: {} with target width: {}, quality: {}", 
                file.getName(), targetWidth, quality);

        // 1. Read image
        BufferedImage originalImage = ImageIO.read(file);
        if (originalImage == null) {
            throw new IOException("Failed to read image or unsupported format: " + file.getAbsolutePath());
        }

        // 2. Calculate new dimensions
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        
        // Skip processing if image is already smaller than target
        if (originalWidth <= targetWidth) {
            log.info("Image {} is already smaller than target width, skipping resize", file.getName());
            // Still compress if quality is less than 1.0
            if (quality < 1.0f) {
                compressImage(file, originalImage, quality);
            }
            return;
        }
        
        int targetHeight = (int) ((double) targetWidth / originalWidth * originalHeight);

        // 3. Resize with high quality
        BufferedImage resizedImage = resizeImage(originalImage, targetWidth, targetHeight);

        // 4. Compress & Save
        saveCompressedImage(file, resizedImage, quality);
        
        log.info("Successfully processed image: {} ({}x{} -> {}x{})", 
                file.getName(), originalWidth, originalHeight, targetWidth, targetHeight);
    }

    /**
     * Resize image with high quality settings
     */
    private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();

        // High quality rendering hints
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);

        // Fill with white background (for transparency handling)
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, targetWidth, targetHeight);
        
        // Draw resized image
        g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();

        return resizedImage;
    }

    /**
     * Compress image without resizing
     */
    private void compressImage(File file, BufferedImage image, float quality) throws IOException {
        saveCompressedImage(file, image, quality);
    }

    /**
     * Save compressed image, overwriting original file
     */
    private void saveCompressedImage(File file, BufferedImage image, float quality) throws IOException {
        // Create temp file in same directory
        File tempFile = new File(file.getParent(), "temp_" + System.currentTimeMillis() + "_" + file.getName());

        try (OutputStream os = new FileOutputStream(tempFile);
             ImageOutputStream ios = ImageIO.createImageOutputStream(os)) {

            // Use JPEG writer for compression
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (!writers.hasNext()) {
                throw new IllegalStateException("No JPEG writers found");
            }

            ImageWriter writer = writers.next();
            writer.setOutput(ios);

            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(Math.max(0.1f, Math.min(1.0f, quality))); // Clamp between 0.1-1.0
            }

            writer.write(null, new IIOImage(image, null, null), param);
            writer.dispose();
        }

        // Replace original file
        if (!file.delete()) {
            tempFile.delete(); // Clean up temp file
            throw new IOException("Failed to delete original file: " + file.getAbsolutePath());
        }
        
        if (!tempFile.renameTo(file)) {
            throw new IOException("Failed to overwrite original file: " + file.getAbsolutePath());
        }
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1).toLowerCase();
    }

    /**
     * Convenience method for processing product images with default settings
     */
    public void processProductImage(File file) throws IOException {
        processImage(file, DEFAULT_PRODUCT_WIDTH, DEFAULT_QUALITY);
    }

    /**
     * Convenience method for processing news images with default settings  
     */
    public void processNewsImage(File file) throws IOException {
        processImage(file, DEFAULT_NEWS_WIDTH, DEFAULT_QUALITY);
    }
}
