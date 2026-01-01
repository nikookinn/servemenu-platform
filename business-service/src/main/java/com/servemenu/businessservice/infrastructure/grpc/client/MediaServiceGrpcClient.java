package com.servemenu.businessservice.infrastructure.grpc.client;

import com.servemenu.mediaservice.grpc.*;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * gRPC Client for Media Service
 * Provides methods to fetch media information from Media Service
 * 
 * Features:
 * - Single media lookup
 * - Batch media lookup (performance optimized)
 * - Automatic timeout handling
 * - Graceful error handling
 * - Round-robin load balancing (configured via application.yml)
 * 
 * @author ServeMenu Platform Team
 * @version 1.0.0
 * @since 2025-11-12
 */
@Slf4j
@Component
public class MediaServiceGrpcClient {
    
    @GrpcClient("media-service")
    private MediaGrpcServiceGrpc.MediaGrpcServiceBlockingStub mediaServiceStub;
    
    /**
     * Get single media by ID
     * 
     * @param mediaId Media asset ID
     * @return Optional containing MediaResponse if found
     */
    public Optional<MediaResponse> getMediaById(UUID mediaId) {
        if (mediaId == null) {
            return Optional.empty();
        }
        
        try {
            long startTime = System.nanoTime();
            log.debug("gRPC Client: Fetching media by ID: {}", mediaId);
            
            GetMediaByIdRequest request = GetMediaByIdRequest.newBuilder()
                    .setMediaId(mediaId.toString())
                    .build();
            
            MediaResponse response = mediaServiceStub
                    .withDeadlineAfter(2, TimeUnit.SECONDS)
                    .getMediaById(request);
            
            long totalTime = System.nanoTime();
            double durationMs = (totalTime - startTime) / 1_000_000.0;
            System.out.println("⚡ [gRPC Client] Single media fetch completed in: " + durationMs + " ms (mediaId: " + mediaId + ")");
            log.debug("gRPC Client: Successfully fetched media: {} in {} ms", mediaId, durationMs);
            return Optional.of(response);
            
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == io.grpc.Status.Code.NOT_FOUND) {
                log.warn("gRPC Client: Media not found: {}", mediaId);
                return Optional.empty();
            }
            log.error("gRPC Client: Error fetching media {}: {}", mediaId, e.getMessage());
            return Optional.empty(); // Graceful degradation
        } catch (Exception e) {
            log.error("gRPC Client: Unexpected error fetching media {}: {}", mediaId, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * Get multiple media by IDs (batch operation - more efficient)
     * 
     * @param mediaIds List of media asset IDs
     * @return Map of media_id -> MediaResponse
     */
    public Map<UUID, MediaResponse> getMediaByIds(List<UUID> mediaIds) {
        if (mediaIds == null || mediaIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        // Filter out null IDs
        List<UUID> validIds = mediaIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        
        if (validIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        try {
            long startTime = System.nanoTime();
            System.out.println("🔍 [gRPC Client] Fetching " + validIds.size() + " media assets in batch: " + validIds);
            log.debug("gRPC Client: Fetching {} media assets in batch", validIds.size());
            
            GetMediaByIdsRequest request = GetMediaByIdsRequest.newBuilder()
                    .addAllMediaIds(validIds.stream()
                            .map(UUID::toString)
                            .collect(Collectors.toList()))
                    .build();
            
            long requestBuiltTime = System.nanoTime();
            System.out.println("⏱️ [gRPC Client] Request built in: " + (requestBuiltTime - startTime) / 1_000_000.0 + " ms");
            
            GetMediaByIdsResponse response = mediaServiceStub
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .getMediaByIds(request);
            
            long responseReceivedTime = System.nanoTime();
            System.out.println("⚡ [gRPC Client] Response received in: " + (responseReceivedTime - requestBuiltTime) / 1_000_000.0 + " ms");
            
            Map<UUID, MediaResponse> resultMap = response.getMediaMapMap().entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> UUID.fromString(e.getKey()),
                            Map.Entry::getValue
                    ));
            
            long totalTime = System.nanoTime();
            System.out.println("✅ [gRPC Client] Successfully fetched " + resultMap.size() + " media assets, " + response.getNotFoundIdsCount() + " not found");
            System.out.println("✅ [gRPC Client] Result map: " + resultMap.keySet());
            System.out.println("📊 [gRPC Client] TOTAL TIME: " + (totalTime - startTime) / 1_000_000.0 + " ms");
            log.debug("gRPC Client: Successfully fetched {} media assets, {} not found in {} ms", 
                    resultMap.size(), response.getNotFoundIdsCount(), (totalTime - startTime) / 1_000_000.0);
            
            return resultMap;
            
        } catch (StatusRuntimeException e) {
            System.out.println("❌ [gRPC Client] Error fetching media batch: " + e.getMessage());
            log.error("gRPC Client: Error fetching media batch: {}", e.getMessage());
            return Collections.emptyMap(); // Graceful degradation
        } catch (Exception e) {
            System.out.println("❌ [gRPC Client] Unexpected error fetching media batch: " + e.getMessage());
            log.error("gRPC Client: Unexpected error fetching media batch: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }
    
    /**
     * Get all media for an entity
     * 
     * @param entityId Entity ID
     * @return List of MediaResponse
     */
    public List<MediaResponse> getMediaByEntityId(UUID entityId) {
        if (entityId == null) {
            return Collections.emptyList();
        }
        
        try {
            log.debug("gRPC Client: Fetching media for entity: {}", entityId);
            
            GetMediaByEntityIdRequest request = GetMediaByEntityIdRequest.newBuilder()
                    .setEntityId(entityId.toString())
                    .build();
            
            GetMediaByEntityIdResponse response = mediaServiceStub
                    .withDeadlineAfter(3, TimeUnit.SECONDS)
                    .getMediaByEntityId(request);
            
            log.debug("gRPC Client: Successfully fetched {} media assets for entity: {}", 
                    response.getTotalCount(), entityId);
            
            return response.getMediaListList();
            
        } catch (StatusRuntimeException e) {
            log.error("gRPC Client: Error fetching media for entity {}: {}", entityId, e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("gRPC Client: Unexpected error fetching media for entity {}: {}", entityId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Check if media exists
     * 
     * @param mediaId Media asset ID
     * @return true if exists, false otherwise
     */
    public boolean mediaExists(UUID mediaId) {
        if (mediaId == null) {
            return false;
        }
        
        try {
            log.debug("gRPC Client: Checking if media exists: {}", mediaId);
            
            MediaExistsRequest request = MediaExistsRequest.newBuilder()
                    .setMediaId(mediaId.toString())
                    .build();
            
            MediaExistsResponse response = mediaServiceStub
                    .withDeadlineAfter(1, TimeUnit.SECONDS)
                    .mediaExists(request);
            
            return response.getExists();
            
        } catch (StatusRuntimeException e) {
            log.error("gRPC Client: Error checking media existence {}: {}", mediaId, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("gRPC Client: Unexpected error checking media existence {}: {}", mediaId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Extract thumbnail URL from MediaResponse
     * 
     * @param mediaResponse MediaResponse from gRPC
     * @return Thumbnail URL or null
     */
    public String getThumbnailUrl(MediaResponse mediaResponse) {
        return mediaResponse != null ? mediaResponse.getThumbnailUrl() : null;
    }
    
    /**
     * Extract large image URL from MediaResponse
     * Large size is optimized for display (QR:800x800, Items:800x800, Logo:1024x1024, Cover:1920x1080)
     * 
     * @param mediaResponse MediaResponse from gRPC
     * @return Large image URL or null
     */
    public String getLargeImageUrl(MediaResponse mediaResponse) {
        return mediaResponse != null ? mediaResponse.getLargeUrl() : null;
    }
    
    /**
     * Extract original image URL from MediaResponse
     * Original size is unprocessed file (for download purposes)
     * 
     * @param mediaResponse MediaResponse from gRPC
     * @return Original image URL or null
     */
    public String getOriginalImageUrl(MediaResponse mediaResponse) {
        return mediaResponse != null ? mediaResponse.getOriginalUrl() : null;
    }
    
    /**
     * Delete media asset (soft delete)
     * Used when logo is changed or removed from QR customization
     * 
     * @param mediaId Media asset ID to delete
     * @return true if deleted successfully, false otherwise
     */
    public boolean deleteMedia(UUID mediaId) {
        if (mediaId == null) {
            log.warn("gRPC Client: Attempted to delete null mediaId");
            return false;
        }
        
        try {
            log.info("gRPC Client: Deleting media: {}", mediaId);
            
            DeleteMediaRequest request = DeleteMediaRequest.newBuilder()
                    .setMediaId(mediaId.toString())
                    .build();
            
            DeleteMediaResponse response = mediaServiceStub
                    .withDeadlineAfter(3, TimeUnit.SECONDS)
                    .deleteMedia(request);
            
            if (response.getSuccess()) {
                log.info("gRPC Client: Successfully deleted media: {}", mediaId);
                return true;
            } else {
                log.warn("gRPC Client: Failed to delete media: {} - {}", mediaId, response.getMessage());
                return false;
            }
            
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == io.grpc.Status.Code.NOT_FOUND) {
                log.warn("gRPC Client: Media not found for deletion: {}", mediaId);
                return false;
            }
            log.error("gRPC Client: Error deleting media {}: {}", mediaId, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("gRPC Client: Unexpected error deleting media {}: {}", mediaId, e.getMessage(), e);
            return false;
        }
    }
}
