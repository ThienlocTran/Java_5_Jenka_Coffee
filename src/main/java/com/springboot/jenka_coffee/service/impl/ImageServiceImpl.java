package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.service.ImageService;
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

@Service
public class ImageServiceImpl implements ImageService {

    @Override
    public void processImage(String filePath, int targetWidth, float quality) throws IOException {
        processImage(new File(filePath), targetWidth, quality);
    }

    @Override
    public void processImage(File file, int targetWidth, float quality) throws IOException {
        if (!file.exists()) {
            throw new IOException("File not found: " + file.getAbsolutePath());
        }

        // 1. Read image
        BufferedImage originalImage = ImageIO.read(file);
        if (originalImage == null) {
            throw new IOException("Failed to read image: " + file.getAbsolutePath());
        }

        // 2. Calculate new dimensions
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        int targetHeight = (int) ((double) targetWidth / originalWidth * originalHeight);

        // If original is smaller than target, keep original dimensions but still
        // compress
        if (originalWidth < targetWidth) {
            targetWidth = originalWidth;
            targetHeight = originalHeight;
        }

        // 3. Resize
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();

        // High quality hints
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw resized (handle transparency check if PNG, but here enforcing RGB/JPG
        // for consistency/compression)
        // If need to support PNG transparency,TYPE_INT_ARGB should be used and
        // different writer.
        // Assuming Photo/News often JPG. If PNG passed, black background might appear
        // with RGB.
        // Let's stick to RGB for standard compression utility as requested (usually for
        // photos).
        g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, Color.WHITE, null);
        g2d.dispose();

        // 4. Compress & Overwrite
        // We write to a temp file first, then replace original
        File tempFile = new File(file.getParent(), "temp_" + file.getName());

        String extension = getFileExtension(file.getName());
        // Force JPG/JPEG writer for compression support (PNG usually generic
        // compression)
        // If extension is PNG, we might lose transparency if we don't handle it,
        // but "Compress" quality param usually applies to JPEG writer.

        try (OutputStream os = new FileOutputStream(tempFile);
                ImageOutputStream ios = ImageIO.createImageOutputStream(os)) {

            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (!writers.hasNext())
                throw new IllegalStateException("No writers found");

            ImageWriter writer = writers.next();
            writer.setOutput(ios);

            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality); // 0.0 - 1.0
            }

            writer.write(null, new IIOImage(resizedImage, null, null), param);
            writer.dispose();
        }

        // 5. Replace original
        if (file.delete()) {
            if (!tempFile.renameTo(file)) {
                // Try copy if rename fails logic could go here, but usually rename works in
                // same dir
                throw new IOException("Failed to overwrite original file");
            }
        } else {
            throw new IOException("Failed to delete original file");
        }
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1).toLowerCase();
    }
}
