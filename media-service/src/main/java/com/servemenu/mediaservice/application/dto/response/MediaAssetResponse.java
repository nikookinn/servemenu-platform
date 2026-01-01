package com.servemenu.mediaservice.application.dto.response;

import com.servemenu.mediaservice.domain.enums.MediaType;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for media asset information
 */
@Builder
public record MediaAssetResponse(
    UUID id,
    String originalFilename,
    
    // Multiple URL sizes for optimized delivery
    String thumbnailUrl,      // QR:500x500, Items:200x200, Logo:200x200, Cover:400x225
    String mediumUrl,         // Items:400x400 (customer app list) - null for other types
    String largeUrl,          // QR:800x800, Items:800x800, Logo:1024x1024, Cover:1920x1080
    String originalUrl,       // Original size - for download
    
    MediaType mediaType,
    String contentType,
    Long fileSize,
    String formattedFileSize,
    Integer width,
    Integer height,
    UUID entityId,
    UUID uploadedBy,
    Instant createdAt
) {}
