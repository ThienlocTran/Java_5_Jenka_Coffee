package com.springboot.jenka_coffee.service;

import java.io.File;
import java.io.IOException;

public interface ImageService {
    /**
     * Rename, Resize and Compress image, then overwrite original file
     * 
     * @param file        Original file
     * @param targetWidth Width to resize to (height calculated automatically to
     *                    maintain aspect ratio)
     * @param quality     Compression quality (0.0 - 1.0)
     * @throws IOException
     */
    void processImage(File file, int targetWidth, float quality) throws IOException;

    /**
     * Process image at specified path
     */
    void processImage(String filePath, int targetWidth, float quality) throws IOException;
}
