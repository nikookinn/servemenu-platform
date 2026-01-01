package com.servemenu.mediaservice.application.dto.response;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for list of media assets
 */
@Builder
public record MediaListResponse(
    UUID entityId,
    long totalCount,
    long totalSize,
    String formattedTotalSize,
    List<MediaAssetResponse> media
) {}
