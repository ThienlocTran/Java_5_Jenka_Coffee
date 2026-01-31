package com.springboot.jenka_coffee.service;

import java.io.File;
import java.io.IOException;

public interface ImageService {
    /**
     * Resize and compress image, then overwrite original file
     * 
     * @param file        Original file
     * @param targetWidth Width to resize to (height calculated automatically to maintain aspect ratio)
     * @param quality     Compression quality (0.1 - 1.0, where 1.0 is highest quality)
     * @throws IOException if processing fails
     */
    void processImage(File file, int targetWidth, float quality) throws IOException;

    /**
     * Process image at specified path
     * 
     * @param filePath    Path to image file
     * @param targetWidth Width to resize to
     * @param quality     Compression quality (0.1 - 1.0)
     * @throws IOException if processing fails
     */
    void processImage(String filePath, int targetWidth, float quality) throws IOException;

    /**
     * Process product image with default settings (800px width, 0.85 quality)
     * 
     * @param file Product image file
     * @throws IOException if processing fails
     */
    void processProductImage(File file) throws IOException;

    /**
     * Process news image with default settings (1200px width, 0.85 quality)
     * 
     * @param file News image file  
     * @throws IOException if processing fails
     */
    void processNewsImage(File file) throws IOException;
}
