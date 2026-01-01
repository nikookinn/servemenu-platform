package com.servemenu.mediaservice.application.service;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Service for image processing operations
 * Handles thumbnail generation, resizing, and optimization
 * 
 * @author ServeMenu Platform Team
 * @version 1.0.0
 * @since 2025-11-12
 */
@Slf4j
@Service
public class ImageProcessingService {
    
    // Compression quality (optimized per use case)
    public static final float HIGH_QUALITY = 0.90f;        // 90% - Logo, QR codes (small files)
    public static final float MEDIUM_QUALITY = 0.80f;      // 80% - Cover images (good balance)
    public static final float THUMBNAIL_QUALITY = 0.75f;   // 75% - Thumbnails (smaller files)
    
    // QR Code sizes (WiFi, Table)
    public static final int QR_SMALL_SIZE = 200;           // 200x200 for quick preview (card icon)
    public static final int QR_MEDIUM_SIZE = 500;          // 500x500 for modal preview
    public static final int QR_LARGE_SIZE = 800;           // 800x800 for download
    
    // Menu Item sizes
    public static final int ITEM_THUMBNAIL_SIZE = 200;     // 200x200 for dashboard cards
    public static final int ITEM_MEDIUM_SIZE = 400;        // 400x400 for customer app list
    public static final int ITEM_LARGE_SIZE = 800;         // 800x800 for customer app detail
    public static final int ITEM_ORIGINAL_SIZE = 1200;     // 1200x1200 for download/zoom
    
    // Logo sizes
    public static final int LOGO_THUMBNAIL_SIZE = 200;     // 200x200 for sidebar, cards
    public static final int LOGO_FULL_SIZE = 1024;         // 1024x1024 for settings page
    
    // Cover Image sizes (16:9 aspect ratio)
    public static final int COVER_THUMBNAIL_WIDTH = 400;   // 400x225 for cards
    public static final int COVER_THUMBNAIL_HEIGHT = 225;
    public static final int COVER_FULL_WIDTH = 1920;       // 1920x1080 for detail view
    public static final int COVER_FULL_HEIGHT = 1080;
    
    /**
     * Resize image to specified dimensions with default quality
     * Maintains aspect ratio
     * 
     * @param originalFile Original image file
     * @param width Target width
     * @param height Target height
     * @return Resized image as byte array
     * @throws IOException if processing fails
     */
    public byte[] resizeImage(MultipartFile originalFile, int width, int height) throws IOException {
        return resizeImage(originalFile, width, height, MEDIUM_QUALITY);
    }
    
    /**
     * Resize image to specified dimensions with custom quality
     * Maintains aspect ratio
     * 
     * @param originalFile Original image file
     * @param width Target width
     * @param height Target height
     * @param quality Compression quality (0.0 to 1.0)
     * @return Resized image as byte array
     * @throws IOException if processing fails
     */
    public byte[] resizeImage(MultipartFile originalFile, int width, int height, float quality) throws IOException {
        return resizeImage(originalFile, width, height, quality, "jpg");  // Default to JPEG
    }
    
    /**
     * Resize image to specified dimensions with custom quality and format
     * Maintains aspect ratio
     * 
     * @param originalFile Original image file
     * @param width Target width
     * @param height Target height
     * @param quality Compression quality (0.0 to 1.0)
     * @param format Output format (jpg, webp, png)
     * @return Resized image as byte array
     * @throws IOException if processing fails
     */
    public byte[] resizeImage(MultipartFile originalFile, int width, int height, float quality, String format) throws IOException {
        log.debug("Resizing image to {}x{} ({}): {}", width, height, format, originalFile.getOriginalFilename());
        
        // For WebP, use ImageIO directly since Thumbnailator doesn't support it well
        if ("webp".equalsIgnoreCase(format)) {
            return resizeToWebP(originalFile, width, height, quality);
        }
        
        try (InputStream inputStream = originalFile.getInputStream();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            Thumbnails.of(inputStream)
                    .size(width, height)
                    .outputQuality(quality)
                    .outputFormat(format)
                    .toOutputStream(outputStream);
            
            byte[] resizedBytes = outputStream.toByteArray();
            log.debug("Image resized successfully ({}). Size: {} bytes, Quality: {}", format, resizedBytes.length, quality);
            
            return resizedBytes;
            
        } catch (IOException e) {
            log.error("Failed to resize image: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Resize image to WebP format using ImageIO
     * Thumbnailator doesn't support WebP, so we use ImageIO with TwelveMonkeys plugin
     */
    private byte[] resizeToWebP(MultipartFile originalFile, int width, int height, float quality) throws IOException {
        try (InputStream inputStream = originalFile.getInputStream();
             ByteArrayOutputStream tempStream = new ByteArrayOutputStream()) {
            
            // First resize with Thumbnailator to PNG (lossless intermediate)
            Thumbnails.of(inputStream)
                    .size(width, height)
                    .outputFormat("png")
                    .toOutputStream(tempStream);
            
            // Then convert PNG to WebP using ImageIO (TwelveMonkeys plugin)
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(tempStream.toByteArray()));
            
            ByteArrayOutputStream webpStream = new ByteArrayOutputStream();
            boolean written = ImageIO.write(image, "webp", webpStream);
            
            if (!written) {
                log.warn("WebP writer not available, falling back to JPEG");
                webpStream.reset();
                ImageIO.write(image, "jpg", webpStream);
            }
            
            byte[] result = webpStream.toByteArray();
            log.debug("Image resized to WebP. Size: {} bytes", result.length);
            return result;
            
        } catch (IOException e) {
            log.error("Failed to resize to WebP: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Resize image from byte array with default quality
     * 
     * @param imageBytes Original image bytes
     * @param width Target width
     * @param height Target height
     * @return Resized image as byte array
     * @throws IOException if processing fails
     */
    public byte[] resizeImage(byte[] imageBytes, int width, int height) throws IOException {
        return resizeImage(imageBytes, width, height, MEDIUM_QUALITY);
    }
    
    /**
     * Resize image from byte array with custom quality
     * 
     * @param imageBytes Original image bytes
     * @param width Target width
     * @param height Target height
     * @param quality Compression quality (0.0 to 1.0)
     * @return Resized image as byte array
     * @throws IOException if processing fails
     */
    public byte[] resizeImage(byte[] imageBytes, int width, int height, float quality) throws IOException {
        return resizeImage(imageBytes, width, height, quality, "jpg");  // Default to JPEG
    }
    
    /**
     * Resize image from byte array with custom quality and format
     * 
     * @param imageBytes Original image bytes
     * @param width Target width
     * @param height Target height
     * @param quality Compression quality (0.0 to 1.0)
     * @param format Output format (jpg, webp, png)
     * @return Resized image as byte array
     * @throws IOException if processing fails
     */
    public byte[] resizeImage(byte[] imageBytes, int width, int height, float quality, String format) throws IOException {
        log.debug("Resizing image from byte array to {}x{} ({}). Size: {} bytes", width, height, format, imageBytes.length);
        
        // For WebP, use ImageIO directly since Thumbnailator doesn't support it well
        if ("webp".equalsIgnoreCase(format)) {
            return resizeToWebPFromBytes(imageBytes, width, height, quality);
        }
        
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            Thumbnails.of(inputStream)
                    .size(width, height)
                    .outputQuality(quality)
                    .outputFormat(format)
                    .toOutputStream(outputStream);
            
            byte[] resizedBytes = outputStream.toByteArray();
            log.debug("Image resized successfully ({}). Original: {} bytes, Resized: {} bytes, Quality: {}", 
                format, imageBytes.length, resizedBytes.length, quality);
            
            return resizedBytes;
            
        } catch (IOException e) {
            log.error("Failed to resize image from bytes: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Resize image bytes to WebP format using ImageIO
     */
    private byte[] resizeToWebPFromBytes(byte[] imageBytes, int width, int height, float quality) throws IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
             ByteArrayOutputStream tempStream = new ByteArrayOutputStream()) {
            
            // First resize with Thumbnailator to PNG (lossless intermediate)
            Thumbnails.of(inputStream)
                    .size(width, height)
                    .outputFormat("png")
                    .toOutputStream(tempStream);
            
            // Then convert PNG to WebP using ImageIO (TwelveMonkeys plugin)
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(tempStream.toByteArray()));
            
            ByteArrayOutputStream webpStream = new ByteArrayOutputStream();
            boolean written = ImageIO.write(image, "webp", webpStream);
            
            if (!written) {
                log.warn("WebP writer not available, falling back to JPEG");
                webpStream.reset();
                ImageIO.write(image, "jpg", webpStream);
            }
            
            byte[] result = webpStream.toByteArray();
            log.debug("Image resized to WebP from bytes. Original: {} bytes, Result: {} bytes", imageBytes.length, result.length);
            return result;
            
        } catch (IOException e) {
            log.error("Failed to resize to WebP from bytes: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    
    
    /**
     * Check if file is an image
     * 
     * @param contentType MIME type
     * @return true if image, false otherwise
     */
    public boolean isImage(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }
    
    /**
     * Get file extension from content type
     * 
     * @param contentType MIME type
     * @return File extension (e.g., "jpg", "png", "webp")
     */
    public String getExtensionFromContentType(String contentType) {
        if (contentType == null) {
            return "jpg";  // Default to JPEG
        }
        
        return switch (contentType.toLowerCase()) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            case "image/avif" -> "avif";
            case "image/svg+xml" -> "svg";
            default -> "jpg";
        };
    }
}
