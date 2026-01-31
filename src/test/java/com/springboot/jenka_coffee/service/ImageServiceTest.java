package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.service.impl.ImageServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ImageServiceTest {

    private final ImageService imageService = new ImageServiceImpl();

    @TempDir
    Path tempDir;

    @Test
    void testProcessImage_ShouldResizeAndCompress() throws IOException {
        // Create a test image
        BufferedImage testImage = new BufferedImage(1000, 800, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = testImage.createGraphics();
        g2d.setColor(Color.BLUE);
        g2d.fillRect(0, 0, 1000, 800);
        g2d.dispose();

        // Save test image to temp file
        File testFile = tempDir.resolve("test_image.jpg").toFile();
        ImageIO.write(testImage, "jpg", testFile);

        long originalSize = testFile.length();
        
        // Process the image
        imageService.processImage(testFile, 500, 0.7f);

        // Verify the image was processed
        assertTrue(testFile.exists());
        
        // Read processed image and check dimensions
        BufferedImage processedImage = ImageIO.read(testFile);
        assertNotNull(processedImage);
        assertEquals(500, processedImage.getWidth());
        assertEquals(400, processedImage.getHeight()); // Aspect ratio maintained

        // File size should be smaller due to compression
        long processedSize = testFile.length();
        assertTrue(processedSize < originalSize, "Processed image should be smaller");
    }

    @Test
    void testProcessImage_SmallerImageShouldNotResize() throws IOException {
        // Create a small test image (smaller than target)
        BufferedImage testImage = new BufferedImage(300, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = testImage.createGraphics();
        g2d.setColor(Color.RED);
        g2d.fillRect(0, 0, 300, 200);
        g2d.dispose();

        File testFile = tempDir.resolve("small_image.jpg").toFile();
        ImageIO.write(testImage, "jpg", testFile);

        // Process with target larger than original
        imageService.processImage(testFile, 800, 0.8f);

        // Image dimensions should remain the same
        BufferedImage processedImage = ImageIO.read(testFile);
        assertNotNull(processedImage);
        assertEquals(300, processedImage.getWidth());
        assertEquals(200, processedImage.getHeight());
    }

    @Test
    void testProcessImage_NonExistentFile_ShouldThrowException() {
        File nonExistentFile = new File("non_existent_file.jpg");
        
        assertThrows(IOException.class, () -> {
            imageService.processImage(nonExistentFile, 500, 0.8f);
        });
    }

    @Test
    void testProcessProductImage_ShouldUseDefaultSettings() throws IOException {
        // Create test image
        BufferedImage testImage = new BufferedImage(1200, 900, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = testImage.createGraphics();
        g2d.setColor(Color.GREEN);
        g2d.fillRect(0, 0, 1200, 900);
        g2d.dispose();

        File testFile = tempDir.resolve("product_image.jpg").toFile();
        ImageIO.write(testImage, "jpg", testFile);

        // Process as product image
        imageService.processProductImage(testFile);

        // Should be resized to 800px width
        BufferedImage processedImage = ImageIO.read(testFile);
        assertNotNull(processedImage);
        assertEquals(800, processedImage.getWidth());
        assertEquals(600, processedImage.getHeight()); // Aspect ratio maintained
    }
}