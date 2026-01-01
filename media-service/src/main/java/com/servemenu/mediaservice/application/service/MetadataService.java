package com.servemenu.mediaservice.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Service for extracting metadata from media files.
 * Extracts information like dimensions, file size, etc.
 */
@Slf4j
@Service
public class MetadataService {
    
    /**
     * Extract image dimensions from a file
     * 
     * @param file MultipartFile
     * @return ImageDimensions containing width and height
     * @throws IOException if reading fails
     */
    public ImageDimensions extractImageDimensions(MultipartFile file) throws IOException {
        log.debug("Extracting image dimensions from file: {}", file.getOriginalFilename());
        
        BufferedImage image = ImageIO.read(file.getInputStream());
        
        if (image == null) {
            log.warn("Failed to read image, returning null dimensions");
            return new ImageDimensions(null, null);
        }
        
        int width = image.getWidth();
        int height = image.getHeight();
        
        log.debug("Extracted dimensions: {}x{}", width, height);
        
        return new ImageDimensions(width, height);
    }
    
    /**
     * Extract file extension from filename
     * 
     * @param filename Original filename
     * @return File extension (lowercase, without dot)
     */
    public String extractFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
    
    /**
     * Generate a clean filename from original filename
     * Removes special characters and spaces
     * 
     * @param originalFilename Original filename
     * @return Cleaned filename
     */
    public String generateCleanFilename(String originalFilename) {
        if (originalFilename == null) {
            return "unnamed";
        }
        
        // Remove extension
        String nameWithoutExt = originalFilename.contains(".") 
            ? originalFilename.substring(0, originalFilename.lastIndexOf("."))
            : originalFilename;
        
        // Replace special characters and spaces with hyphens
        String cleaned = nameWithoutExt
            .toLowerCase()
            .replaceAll("[^a-z0-9]", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
        
        // Limit length
        if (cleaned.length() > 50) {
            cleaned = cleaned.substring(0, 50);
        }
        
        return cleaned.isEmpty() ? "unnamed" : cleaned;
    }
    
    /**
     * Calculate aspect ratio
     * 
     * @param width Image width
     * @param height Image height
     * @return Aspect ratio as string (e.g., "16:9", "1:1")
     */
    public String calculateAspectRatio(int width, int height) {
        if (width <= 0 || height <= 0) {
            return "unknown";
        }
        
        int gcd = gcd(width, height);
        int ratioWidth = width / gcd;
        int ratioHeight = height / gcd;
        
        return ratioWidth + ":" + ratioHeight;
    }
    
    /**
     * Calculate greatest common divisor
     */
    private int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }
    
    /**
     * Format file size to human-readable string
     * 
     * @param bytes File size in bytes
     * @return Formatted string (e.g., "1.5 MB")
     */
    public String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
    
    /**
     * Data class for image dimensions
     */
    public record ImageDimensions(Integer width, Integer height) {
        public boolean isValid() {
            return width != null && height != null && width > 0 && height > 0;
        }
    }
}
