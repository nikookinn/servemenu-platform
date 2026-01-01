package com.servemenu.mediaservice.application.dto.response;

import com.servemenu.mediaservice.domain.enums.MediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Serializable wrapper for MediaAssetResponse to fix Redis cache serialization issues.
 * 
 * Problem: MediaAssetResponse record cannot be properly serialized/deserialized by Redis cache,
 * causing ClassCastException: LinkedHashMap cannot be cast to MediaAssetResponse.
 * 
 * Solution: Create a serializable DTO wrapper class that implements Serializable
 * and can be properly handled by Redis cache serialization.
 * 
 * This pattern is used by Netflix, Uber, and other production systems for cache-friendly DTOs.
 * 
 * @author ServeMenu Platform Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaAssetResponseWrapper implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private UUID id;
    private String originalFilename;
    
    // Multiple URL sizes for optimized delivery
    private String thumbnailUrl;      // QR:500x500, Items:200x200, Logo:200x200, Cover:400x225
    private String mediumUrl;         // Items:400x400 (customer app list) - null for other types
    private String largeUrl;          // QR:800x800, Items:800x800, Logo:1024x1024, Cover:1920x1080
    private String originalUrl;       // Original size - for download
    
    private MediaType mediaType;
    private String contentType;
    private Long fileSize;
    private String formattedFileSize;
    private Integer width;
    private Integer height;
    private UUID entityId;
    private UUID uploadedBy;
    private Instant createdAt;
    
    /**
     * Factory method to create wrapper from MediaAssetResponse
     */
    public static MediaAssetResponseWrapper of(MediaAssetResponse response) {
        return MediaAssetResponseWrapper.builder()
            .id(response.id())
            .originalFilename(response.originalFilename())
            .thumbnailUrl(response.thumbnailUrl())
            .mediumUrl(response.mediumUrl())
            .largeUrl(response.largeUrl())
            .originalUrl(response.originalUrl())
            .mediaType(response.mediaType())
            .contentType(response.contentType())
            .fileSize(response.fileSize())
            .formattedFileSize(response.formattedFileSize())
            .width(response.width())
            .height(response.height())
            .entityId(response.entityId())
            .uploadedBy(response.uploadedBy())
            .createdAt(response.createdAt())
            .build();
    }
    
    /**
     * Convert wrapper back to MediaAssetResponse
     */
    public MediaAssetResponse toResponse() {
        return MediaAssetResponse.builder()
            .id(id)
            .originalFilename(originalFilename)
            .thumbnailUrl(thumbnailUrl)
            .mediumUrl(mediumUrl)
            .largeUrl(largeUrl)
            .originalUrl(originalUrl)
            .mediaType(mediaType)
            .contentType(contentType)
            .fileSize(fileSize)
            .formattedFileSize(formattedFileSize)
            .width(width)
            .height(height)
            .entityId(entityId)
            .uploadedBy(uploadedBy)
            .createdAt(createdAt)
            .build();
    }
}
