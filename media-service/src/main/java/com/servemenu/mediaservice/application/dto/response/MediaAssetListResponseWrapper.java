package com.servemenu.mediaservice.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serializable wrapper for List<MediaAssetResponse> to fix Redis cache serialization issues.
 * 
 * Problem: List<MediaAssetResponse> cannot be properly serialized/deserialized by Redis cache,
 * causing ClassCastException: LinkedHashMap cannot be cast to MediaAssetResponse.
 * 
 * Solution: Create a serializable DTO wrapper class that implements Serializable
 * and can be properly handled by Redis cache serialization.
 * 
 * @author ServeMenu Platform Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaAssetListResponseWrapper implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private List<MediaAssetResponseWrapper> mediaAssets;
    
    /**
     * Factory method to create wrapper from List<MediaAssetResponse>
     */
    public static MediaAssetListResponseWrapper of(List<MediaAssetResponse> responses) {
        List<MediaAssetResponseWrapper> wrappers = responses.stream()
            .map(MediaAssetResponseWrapper::of)
            .collect(Collectors.toList());
        
        return MediaAssetListResponseWrapper.builder()
            .mediaAssets(wrappers)
            .build();
    }
    
    /**
     * Convert wrapper back to List<MediaAssetResponse>
     */
    public List<MediaAssetResponse> toResponseList() {
        return mediaAssets.stream()
            .map(MediaAssetResponseWrapper::toResponse)
            .collect(Collectors.toList());
    }
}
