package com.servemenu.mediaservice.infrastructure.grpc;

import com.servemenu.mediaservice.application.dto.response.MediaAssetResponse;
import com.servemenu.mediaservice.domain.model.MediaAsset;
import com.servemenu.mediaservice.domain.repository.MediaAssetRepository;
import com.servemenu.mediaservice.grpc.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * gRPC Server Implementation for Media Service
 * Provides inter-service communication for Business Service and other microservices
 * 
 * Features:
 * - Single media lookup
 * - Batch media lookup (optimized for performance)
 * - Entity-based media retrieval
 * - Media existence check
 * 
 * @author ServeMenu Platform Team
 * @version 1.0.0
 * @since 2025-11-12
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class MediaGrpcServiceImpl extends MediaGrpcServiceGrpc.MediaGrpcServiceImplBase {

    private final MediaAssetRepository mediaAssetRepository;
    private final com.servemenu.mediaservice.application.service.MediaService mediaService;
    
    @Value("${aws.s3.cdn-url:http://localhost:4566/servemenu-media-dev}")
    private String cdnBaseUrl;

    /**
     * Get single media asset by ID
     * USES CACHE: Calls MediaService which has @Cacheable
     * 
     * @param request GetMediaByIdRequest containing media_id
     * @param responseObserver StreamObserver for MediaResponse
     */
    @Override
    public void getMediaById(
            GetMediaByIdRequest request,
            StreamObserver<MediaResponse> responseObserver) {
        
        try {
            log.debug("gRPC: Received GetMediaById request for mediaId: {}", request.getMediaId());
            
            UUID mediaId = UUID.fromString(request.getMediaId());
            
            // Use MediaService to leverage cache ✅
            MediaAssetResponse mediaResponse = mediaService.getMediaById(mediaId);
            
            // Convert to gRPC response
            MediaResponse response = MediaResponse.newBuilder()
                .setMediaId(mediaResponse.id().toString())
                .setOriginalFileName(mediaResponse.originalFilename())
                .setFileSize(mediaResponse.fileSize())
                .setMimeType(mediaResponse.contentType())
                .setMediaType(mediaResponse.mediaType().name())
                .setEntityId(mediaResponse.entityId().toString())
                .setThumbnailUrl(mediaResponse.thumbnailUrl())
                .setMediumUrl(mediaResponse.mediumUrl() != null ? mediaResponse.mediumUrl() : "")
                .setLargeUrl(mediaResponse.largeUrl())
                .setOriginalUrl(mediaResponse.originalUrl())
                .setCreatedAt(mediaResponse.createdAt().getEpochSecond())
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
            log.debug("gRPC: Successfully returned media from cache/DB for id: {}", mediaId);
            
        } catch (com.servemenu.mediaservice.infrastructure.exception.MediaNotFoundException e) {
            log.warn("gRPC: Media not found: {}", request.getMediaId());
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Media not found with ID: " + request.getMediaId())
                    .asRuntimeException());
        } catch (IllegalArgumentException e) {
            log.error("gRPC: Invalid UUID format: {}", request.getMediaId());
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid media ID format")
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("gRPC: Error getting media by ID: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error while fetching media")
                    .asRuntimeException());
        }
    }

    /**
     * Get multiple media assets by IDs (batch operation)
     * This is optimized for performance when fetching multiple media at once
     * 
     * @param request GetMediaByIdsRequest containing list of media_ids
     * @param responseObserver StreamObserver for GetMediaByIdsResponse
     */
    @Override
    @Transactional(readOnly = true)
    public void getMediaByIds(
            GetMediaByIdsRequest request,
            StreamObserver<GetMediaByIdsResponse> responseObserver) {
        
        try {
            log.debug("gRPC: Received GetMediaByIds request for {} media IDs", request.getMediaIdsList().size());
            
            // Convert string IDs to UUIDs
            List<UUID> mediaIds = request.getMediaIdsList().stream()
                    .map(UUID::fromString)
                    .collect(Collectors.toList());
            
            // Fetch all media assets in one query (batch)
            List<MediaAsset> mediaAssets = mediaAssetRepository.findAllByIdInAndDeletedFalse(mediaIds);
            
            // Create response map
            Map<String, MediaResponse> mediaMap = new HashMap<>();
            Set<String> foundIds = new HashSet<>();
            
            for (MediaAsset asset : mediaAssets) {
                String idString = asset.getId().toString();
                mediaMap.put(idString, mapToMediaResponse(asset));
                foundIds.add(idString);
            }
            
            // Find IDs that were not found
            List<String> notFoundIds = request.getMediaIdsList().stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toList());
            
            GetMediaByIdsResponse response = GetMediaByIdsResponse.newBuilder()
                    .putAllMediaMap(mediaMap)
                    .addAllNotFoundIds(notFoundIds)
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
            log.debug("gRPC: Successfully returned {} media assets, {} not found", 
                    mediaMap.size(), notFoundIds.size());
            
        } catch (IllegalArgumentException e) {
            log.error("gRPC: Invalid UUID format in batch request");
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid media ID format in request")
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("gRPC: Error getting media by IDs: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error while fetching media batch")
                    .asRuntimeException());
        }
    }

    /**
     * Get all media assets for a specific entity
     * Optionally filter by media type
     * 
     * @param request GetMediaByEntityIdRequest containing entity_id and optional media_type
     * @param responseObserver StreamObserver for GetMediaByEntityIdResponse
     */
    @Override
    @Transactional(readOnly = true)
    public void getMediaByEntityId(
            GetMediaByEntityIdRequest request,
            StreamObserver<GetMediaByEntityIdResponse> responseObserver) {
        
        try {
            log.debug("gRPC: Received GetMediaByEntityId request for entityId: {}", request.getEntityId());
            
            UUID entityId = UUID.fromString(request.getEntityId());
            List<MediaAsset> mediaAssets;
            
            // Filter by media type if provided
            if (request.hasMediaType() && !request.getMediaType().isEmpty()) {
                mediaAssets = mediaAssetRepository.findByEntityIdAndTypeAndDeletedFalse(
                        entityId, 
                        request.getMediaType()
                );
                log.debug("gRPC: Filtering by media type: {}", request.getMediaType());
            } else {
                mediaAssets = mediaAssetRepository.findByEntityIdAndDeletedFalse(entityId);
            }
            
            List<MediaResponse> mediaList = mediaAssets.stream()
                    .map(this::mapToMediaResponse)
                    .collect(Collectors.toList());
            
            GetMediaByEntityIdResponse response = GetMediaByEntityIdResponse.newBuilder()
                    .addAllMediaList(mediaList)
                    .setTotalCount(mediaList.size())
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
            log.debug("gRPC: Successfully returned {} media assets for entity: {}", 
                    mediaList.size(), request.getEntityId());
            
        } catch (IllegalArgumentException e) {
            log.error("gRPC: Invalid UUID format: {}", request.getEntityId());
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid entity ID format")
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("gRPC: Error getting media by entity ID: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error while fetching entity media")
                    .asRuntimeException());
        }
    }

    /**
     * Check if media exists (lightweight operation)
     * 
     * @param request MediaExistsRequest containing media_id
     * @param responseObserver StreamObserver for MediaExistsResponse
     */
    @Override
    @Transactional(readOnly = true)
    public void mediaExists(
            MediaExistsRequest request,
            StreamObserver<MediaExistsResponse> responseObserver) {
        
        try {
            log.debug("gRPC: Received MediaExists request for mediaId: {}", request.getMediaId());
            
            UUID mediaId = UUID.fromString(request.getMediaId());
            boolean exists = mediaAssetRepository.existsByIdAndDeletedFalse(mediaId);
            
            MediaExistsResponse response = MediaExistsResponse.newBuilder()
                    .setExists(exists)
                    .setMediaId(request.getMediaId())
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
            log.debug("gRPC: Media exists check result: {} for id: {}", exists, request.getMediaId());
            
        } catch (IllegalArgumentException e) {
            log.error("gRPC: Invalid UUID format: {}", request.getMediaId());
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid media ID format")
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("gRPC: Error checking media existence: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error while checking media existence")
                    .asRuntimeException());
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Maps MediaAsset domain entity to gRPC MediaResponse
     * Generates CDN URLs for different image sizes
     * 
     * @param asset MediaAsset domain entity
     * @return MediaResponse gRPC message
     */
    private MediaResponse mapToMediaResponse(MediaAsset asset) {
        // Generate URLs based on S3 keys
        String originalUrl = cdnBaseUrl + "/" + asset.getS3Key();
        
        String thumbnailUrl = asset.getThumbnailS3Key() != null
            ? cdnBaseUrl + "/" + asset.getThumbnailS3Key()
            : originalUrl;  // Fallback to original
        
        String mediumUrl = asset.getMediumS3Key() != null
            ? cdnBaseUrl + "/" + asset.getMediumS3Key()
            : "";  // Empty for non-MENU_ITEM types
        
        String largeUrl = asset.getLargeS3Key() != null
            ? cdnBaseUrl + "/" + asset.getLargeS3Key()
            : originalUrl;  // Fallback to original
        
        return MediaResponse.newBuilder()
                .setMediaId(asset.getId().toString())
                .setOriginalFileName(asset.getOriginalFilename())
                .setFileSize(asset.getFileSize())
                .setMimeType(asset.getContentType())
                .setMediaType(asset.getMediaType().name())
                .setEntityId(asset.getEntityId().toString())
                .setThumbnailUrl(thumbnailUrl)
                .setMediumUrl(mediumUrl)
                .setLargeUrl(largeUrl)
                .setOriginalUrl(originalUrl)
                .setCreatedAt(asset.getCreatedAt().getEpochSecond())
                .build();
    }
    
    /**
     * Delete media asset (soft delete)
     * Removes media from active use but keeps in database for audit
     * 
     * @param request DeleteMediaRequest containing media_id
     * @param responseObserver StreamObserver for DeleteMediaResponse
     */
    @Override
    public void deleteMedia(
            DeleteMediaRequest request,
            StreamObserver<DeleteMediaResponse> responseObserver) {
        
        try {
            log.info("gRPC: Received DeleteMedia request for mediaId: {}", request.getMediaId());
            
            UUID mediaId = UUID.fromString(request.getMediaId());
            
            // Call MediaService to soft delete
            mediaService.deleteMedia(mediaId);
            
            DeleteMediaResponse response = DeleteMediaResponse.newBuilder()
                .setSuccess(true)
                .setMediaId(request.getMediaId())
                .setMessage("Media deleted successfully")
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
            log.info("gRPC: Successfully deleted media: {}", mediaId);
            
        } catch (com.servemenu.mediaservice.infrastructure.exception.MediaNotFoundException e) {
            log.warn("gRPC: Media not found for deletion: {}", request.getMediaId());
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Media not found with ID: " + request.getMediaId())
                    .asRuntimeException());
        } catch (IllegalArgumentException e) {
            log.error("gRPC: Invalid UUID format for deletion: {}", request.getMediaId());
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid media ID format")
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("gRPC: Error deleting media: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error while deleting media")
                    .asRuntimeException());
        }
    }
}
