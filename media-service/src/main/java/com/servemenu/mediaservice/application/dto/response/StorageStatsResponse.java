package com.servemenu.mediaservice.application.dto.response;

import lombok.Builder;

import java.util.UUID;

/**
 * Response DTO for storage statistics
 */
@Builder
public record StorageStatsResponse(
    UUID entityId,
    long fileCount,
    long totalBytes,
    String formattedSize
) {}
