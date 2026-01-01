package com.servemenu.mediaservice.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum representing different types of media assets in the system.
 * Each type has specific size requirements and use cases.
 */
@Getter
@RequiredArgsConstructor
public enum MediaType {
    
    /**
     * Business logo - 256×256 px (1:1 square aspect ratio)
     * Multi-size: thumbnail=64x64, medium=256x256
     * Used in: Navbar (40-60px), profile (80-100px), settings (150-200px)
     * Frontend crops to 256x256 square, backend generates multiple sizes
     * Note: Optimized for small display sizes, Retina-ready (128px @2x)
     */
    LOGO("logos", 256, 256, 300 * 1024), // 300 KB max
    
    /**
     * Business Cover Image - 1200×675 px (16:9 landscape aspect ratio)
     * Multi-size: thumbnail=320x180, medium=800x450, large=1200x675
     * Used in: Business settings, customer app banner, header
     * Frontend crops to 1200x675, backend generates multiple sizes
     * Note: Optimized for typical desktop displays (1200-1440px width)
     */
    COVER_IMAGE("cover-images", 1200, 675, 1 * 1024 * 1024), // 1 MB max
    
    /**
     * Menu item image - 1200×1200 px max
     * Multi-size: thumbnail=200x200, medium=400x400, large=800x800, original=1200x1200
     * Used in: Menu displays, item details
     * Note: Frontend crops to 1200x1200, backend generates multiple sizes
     */
    MENU_ITEM("menu-items", 1200, 1200, 300 * 1024), // 300 KB max
    
    /**
     * Business QR code - 800×800 px max
     * Multi-size: thumbnail=200x200 (quick preview), medium=500x500 (modal), large=800x800 (download)
     * Used in: Business landing page, marketing materials
     * Note: Increased to 300KB to support high-quality QR with frames and customization
     */
    BUSINESS_QR("qr-codes/business", 800, 800, 300 * 1024), // 300 KB max (increased for frames)
    
    /**
     * Table QR code - 800×800 px max
     * Multi-size: thumbnail=200x200 (quick preview), medium=500x500 (modal), large=800x800 (download)
     * Used in: Table ordering, physical table markers
     * Note: Increased to 300KB to support high-quality QR with frames and customization
     */
    TABLE_QR("qr-codes/tables", 800, 800, 300 * 1024), // 300 KB max (increased for frames)
    
    /**
     * WiFi QR code - 800×800 px max
     * Multi-size: thumbnail=200x200 (quick preview), medium=500x500 (modal), large=800x800 (download)
     * Used in: WiFi access, customer convenience
     * Note: Increased to 300KB to support high-quality QR with frames and customization
     */
    WIFI_QR("qr-codes/wifi", 800, 800, 300 * 1024), // 300 KB max (increased for frames)
    
    /**
     * QR Logo - 512×512 px (single size)
     * Only medium size generated: 512x512 for QR embedding
     * Used in: QR code customization, embedded in generated QR codes
     * Note: Backend automatically resizes any uploaded image to 512x512 for better quality
     * Max file size: 2MB (will be resized and compressed)
     */
    QR_LOGO("qr-logos", 512, 512, 2 * 1024 * 1024); // 2 MB max, resized to 512x512

    /**
     * -- GETTER --
     *  Get the S3 key prefix for this media type
     *
     * @return S3 prefix path
     */
    @Getter
    private final String s3Prefix;
    private final int maxWidth;
    private final int maxHeight;
    private final long maxFileSize;

    /**
     * Check if the given dimensions are within acceptable limits
     * @param width Image width
     * @param height Image height
     * @return true if dimensions are acceptable
     */
    public boolean isValidDimensions(int width, int height) {
        return width <= maxWidth && height <= maxHeight && width > 0 && height > 0;
    }
    
    /**
     * Check if the file size is within acceptable limits
     * @param fileSize File size in bytes
     * @return true if file size is acceptable
     */
    public boolean isValidFileSize(long fileSize) {
        return fileSize > 0 && fileSize <= maxFileSize;
    }
}
