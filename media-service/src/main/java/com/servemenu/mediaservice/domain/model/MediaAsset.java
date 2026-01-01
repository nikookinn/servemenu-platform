package com.servemenu.mediaservice.domain.model;

import com.servemenu.mediaservice.domain.enums.MediaType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain entity representing a media asset stored in S3.
 * Uses soft delete pattern for data retention and audit purposes.
 */
@Entity
@Table(
    name = "media_assets",
    indexes = {
        @Index(name = "idx_media_entity_id", columnList = "entity_id"),
        @Index(name = "idx_media_type", columnList = "media_type"),
        @Index(name = "idx_media_created_at", columnList = "created_at"),
        @Index(name = "idx_media_entity_type", columnList = "entity_id, media_type")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaAsset {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Original filename as uploaded by the user
     */
    @Column(name = "original_filename", nullable = false)
    private String originalFilename;
    
    /**
     * S3 key for original image
     * Format: originals/{type}/{entityId}/{timestamp}.{extension}
     */
    @Column(name = "s3_key", nullable = false, unique = true, length = 500)
    private String s3Key;
    
    /**
     * S3 key for thumbnail
     * QR Codes: 500x500, Items: 200x200, Logo: 200x200, Cover: 400x225
     */
    @Column(name = "thumbnail_s3_key", length = 500)
    private String thumbnailS3Key;
    
    /**
     * S3 key for medium size (400x400)
     * Only for MENU_ITEM type
     */
    @Column(name = "medium_s3_key", length = 500)
    private String mediumS3Key;
    
    /**
     * S3 key for large size
     * Items: 800x800, Logo: 1024x1024, Cover: 1920x1080, QR: 800x800
     */
    @Column(name = "large_s3_key", length = 500)
    private String largeS3Key;
    
    /**
     * Full public URL to access the original media asset
     */
    @Column(name = "s3_url", nullable = false, length = 1000)
    private String s3Url;
    
    /**
     * Type of media asset
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 50)
    private MediaType mediaType;
    
    /**
     * MIME type of the file
     */
    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;
    
    /**
     * File size in bytes
     */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;
    
    /**
     * Image width in pixels
     */
    @Column(name = "width")
    private Integer width;
    
    /**
     * Image height in pixels
     */
    @Column(name = "height")
    private Integer height;
    
    /**
     * ID of the entity this media belongs to
     * (e.g., business ID, menu item ID, table ID)
     */
    @Column(name = "entity_id", nullable = false)
    private UUID entityId;
    
    /**
     * ID of the user who uploaded this media
     */
    @Column(name = "uploaded_by")
    private UUID uploadedBy;
    
    /**
     * Timestamp when the media was created
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    /**
     * Soft delete timestamp
     * NULL = active, non-NULL = deleted
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;
    
    // Business Logic Methods
    
    /**
     * Soft delete this media asset
     */
    public void softDelete() {
        this.deletedAt = Instant.now();
    }
    
    /**
     * Restore a soft-deleted media asset
     */
    public void restore() {
        this.deletedAt = null;
    }
    
    /**
     * Check if this media asset is deleted
     * @return true if deleted
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }
    
    /**
     * Check if this media asset is active
     * @return true if active (not deleted)
     */
    public boolean isActive() {
        return this.deletedAt == null;
    }
    
    /**
     * Get file extension from original filename
     * @return file extension (e.g., "avif", "png")
     */
    public String getFileExtension() {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
    }
    
    /**
     * Get human-readable file size
     * @return formatted file size (e.g., "1.5 MB")
     */
    public String getFormattedFileSize() {
        if (fileSize == null) return "0 B";
        
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
    }
}
