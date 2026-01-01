package com.servemenu.mediaservice.application.service;

import com.servemenu.mediaservice.domain.enums.MediaType;
import com.servemenu.mediaservice.infrastructure.exception.InvalidMediaException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Service for validating media files before upload.
 * Ensures files meet size, format, and dimension requirements.
 */
@Slf4j
@Service
public class ValidationService {
    
    private final FileScanningService fileScanningService;
    
    public ValidationService(FileScanningService fileScanningService) {
        this.fileScanningService = fileScanningService;
    }
    
    // Allowed content types
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
        "image/avif",
        "image/webp",
        "image/png",
        "image/jpeg"
    );
    
    // Allowed file extensions
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
        "avif", "webp", "png", "jpg", "jpeg"
    );
    
    // Maximum file size: 10 MB
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    
    /**
     * Validate a media file for upload
     * 
     * @param file MultipartFile to validate
     * @param mediaType Expected media type
     * @throws InvalidMediaException if validation fails
     */
    public void validateMediaFile(MultipartFile file, MediaType mediaType) {
        log.debug("Validating media file: filename={}, size={}, type={}", 
            file.getOriginalFilename(), file.getSize(), file.getContentType());
        
        // Check if file is empty
        if (file.isEmpty()) {
            throw new InvalidMediaException("File is empty");
        }
        
        // Validate filename
        validateFilename(file.getOriginalFilename());
        
        // Validate content type
        validateContentType(file.getContentType());
        
        // Validate file extension
        validateFileExtension(file.getOriginalFilename());
        
        // Validate file size (general limit)
        validateFileSize(file.getSize());
        
        // Validate file size against media type specific limit
        validateFileSizeForMediaType(file.getSize(), mediaType);
        
        // Validate image dimensions
        validateImageDimensions(file, mediaType);
        
        // Scan for malware (ClamAV)
        try {
            fileScanningService.scanFile(file);
        } catch (IOException e) {
            log.error("File scanning failed: {}", e.getMessage(), e);
            throw new InvalidMediaException("File scanning failed. Please try again.", e);
        }
        
        log.debug("Media file validation passed");
    }
    
    /**
     * Validate filename
     */
    private void validateFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new InvalidMediaException("Filename is required");
        }
        
        if (filename.length() > 255) {
            throw new InvalidMediaException("Filename is too long (max 255 characters)");
        }
        
        // Check for path traversal attempts
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new InvalidMediaException("Invalid filename: path traversal detected");
        }
    }
    
    /**
     * Validate content type
     */
    private void validateContentType(String contentType) {
        if (contentType == null || contentType.trim().isEmpty()) {
            throw new InvalidMediaException("Content type is required");
        }
        
        if (!ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidMediaException(
                String.format("Invalid content type: %s. Allowed types: %s", 
                    contentType, String.join(", ", ALLOWED_CONTENT_TYPES))
            );
        }
    }
    
    /**
     * Validate file extension
     */
    private void validateFileExtension(String filename) {
        String extension = getFileExtension(filename);
        
        if (extension.isEmpty()) {
            throw new InvalidMediaException("File extension is required");
        }
        
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidMediaException(
                String.format("Invalid file extension: %s. Allowed extensions: %s", 
                    extension, String.join(", ", ALLOWED_EXTENSIONS))
            );
        }
    }
    
    /**
     * Validate file size (general limit)
     */
    private void validateFileSize(long fileSize) {
        if (fileSize <= 0) {
            throw new InvalidMediaException("File size must be greater than 0");
        }
        
        if (fileSize > MAX_FILE_SIZE) {
            throw new InvalidMediaException(
                String.format("File size exceeds maximum limit: %d bytes (max: %d bytes / 10 MB)", 
                    fileSize, MAX_FILE_SIZE)
            );
        }
    }
    
    /**
     * Validate file size against media type specific limit
     */
    private void validateFileSizeForMediaType(long fileSize, MediaType mediaType) {
        if (!mediaType.isValidFileSize(fileSize)) {
            throw new InvalidMediaException(
                String.format("File size exceeds limit for %s: %d bytes (max: %d bytes)", 
                    mediaType.name(), fileSize, mediaType.getMaxFileSize())
            );
        }
    }
    
    /**
     * Validate image dimensions
     */
    private void validateImageDimensions(MultipartFile file, MediaType mediaType) {
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            
            if (image == null) {
                throw new InvalidMediaException("Failed to read image. File may be corrupted or not a valid image.");
            }
            
            int width = image.getWidth();
            int height = image.getHeight();
            
            log.debug("Image dimensions: {}x{}", width, height);
            
            // Skip dimension validation for QR_LOGO - it will be automatically resized
            if (mediaType != MediaType.QR_LOGO && !mediaType.isValidDimensions(width, height)) {
                throw new InvalidMediaException(
                    String.format("Invalid image dimensions for %s: %dx%d (max: %dx%d)", 
                        mediaType.name(), width, height, 
                        mediaType.getMaxWidth(), mediaType.getMaxHeight())
                );
            }
            
            if (mediaType == MediaType.QR_LOGO) {
                log.info("QR Logo will be automatically resized from {}x{} to max 512x512", width, height);
            }
            
        } catch (IOException e) {
            log.error("Failed to read image dimensions: {}", e.getMessage(), e);
            throw new InvalidMediaException("Failed to read image dimensions. File may be corrupted.", e);
        }
    }
    
    /**
     * Extract file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
    
    /**
     * Check if content type is an image
     */
    public boolean isImageContentType(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }
}
