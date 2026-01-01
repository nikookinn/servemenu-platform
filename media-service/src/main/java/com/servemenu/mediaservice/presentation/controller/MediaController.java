package com.servemenu.mediaservice.presentation.controller;

import com.servemenu.mediaservice.application.dto.response.MediaAssetResponse;
import com.servemenu.mediaservice.application.dto.response.MediaListResponse;
import com.servemenu.mediaservice.application.dto.response.StorageStatsResponse;
import com.servemenu.mediaservice.application.service.MediaService;
import com.servemenu.mediaservice.domain.enums.MediaType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for media asset operations.
 * Provides endpoints for upload, retrieval, and deletion of media files.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {
    
    private final MediaService mediaService;
    private final CacheManager cacheManager;
    
    /**
     * Upload a media file
     * 
     * POST /api/v1/media/upload
     * 
     * @param file File to upload
     * @param type Media type (LOGO, COVER_IMAGE, MENU_ITEM, TABLE_QR, WIFI_QR)
     * @param entityId Entity ID this media belongs to
     * @param uploadedBy User ID who uploaded (optional, from JWT in production)
     * @return MediaAssetResponse with upload details
     */
    @PostMapping(value = "/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<MediaAssetResponse> uploadMedia(
        @RequestParam("file") MultipartFile file,
        @RequestParam("type") MediaType type,
        @RequestParam("entityId") UUID entityId,
        @RequestParam(value = "uploadedBy", required = false) UUID uploadedBy
    ) {
        log.info("Upload request received: type={}, entityId={}, filename={}", 
            type, entityId, file.getOriginalFilename());
        
        MediaAssetResponse response = mediaService.uploadMedia(file, type, entityId, uploadedBy);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Upload QR logo for QR customization
     * Dedicated endpoint for QR logo uploads with simplified parameters
     * Note: Frontend optimizes images before upload (target: 100KB, 512x512 px)
     * 
     * POST /api/v1/media/qr-logo/upload
     * 
     * @param file Logo file to upload (pre-optimized by frontend)
     * @param storeId Store ID this logo belongs to
     * @param uploadedBy User ID who uploaded (optional, from JWT in production)
     * @return MediaAssetResponse with mediaId for QR customization
     */
    @PostMapping(value = "/qr-logo/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<MediaAssetResponse> uploadQRLogo(
        @RequestParam("file") MultipartFile file,
        @RequestParam("storeId") UUID storeId,
        @RequestParam(value = "uploadedBy", required = false) UUID uploadedBy
    ) {
        log.info("QR Logo upload request: storeId={}, filename={}, size={} bytes ({} KB)", 
            storeId, file.getOriginalFilename(), file.getSize(), file.getSize() / 1024);
        
        // Upload with QR_LOGO type (no size validation - frontend handles optimization)
        MediaAssetResponse response = mediaService.uploadMedia(file, MediaType.QR_LOGO, storeId, uploadedBy);
        
        log.info("✅ QR Logo uploaded successfully: mediaId={}, storeId={}, size={} KB", 
            response.id(), storeId, file.getSize() / 1024);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Get media asset by ID
     * 
     * GET /api/v1/media/{id}
     * 
     * @param id Media asset ID
     * @return MediaAssetResponse
     */
    @GetMapping("/{id}")
    public ResponseEntity<MediaAssetResponse> getMediaById(@PathVariable UUID id) {
        log.debug("Get media request: id={}", id);
        
        MediaAssetResponse response = mediaService.getMediaById(id);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all media assets for an entity
     * 
     * GET /api/v1/media/entity/{entityId}
     * 
     * @param entityId Entity ID
     * @return MediaListResponse with all media for the entity
     */
    @GetMapping("/entity/{entityId}")
    public ResponseEntity<MediaListResponse> getMediaByEntityId(@PathVariable UUID entityId) {
        log.debug("Get media by entity request: entityId={}", entityId);
        
        List<MediaAssetResponse> mediaList = mediaService.getMediaByEntityId(entityId);
        MediaService.StorageStats stats = mediaService.getStorageStats(entityId);
        
        MediaListResponse response = MediaListResponse.builder()
            .entityId(entityId)
            .totalCount(stats.fileCount())
            .totalSize(stats.totalBytes())
            .formattedTotalSize(stats.formattedSize())
            .media(mediaList)
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get media assets by entity ID and type
     * 
     * GET /api/v1/media/entity/{entityId}/type/{type}
     * 
     * @param entityId Entity ID
     * @param type Media type
     * @return List of MediaAssetResponse
     */
    @GetMapping("/entity/{entityId}/type/{type}")
    public ResponseEntity<List<MediaAssetResponse>> getMediaByEntityIdAndType(
        @PathVariable UUID entityId,
        @PathVariable MediaType type
    ) {
        log.debug("Get media by entity and type request: entityId={}, type={}", entityId, type);
        
        List<MediaAssetResponse> mediaList = mediaService.getMediaByEntityIdAndType(entityId, type);
        
        return ResponseEntity.ok(mediaList);
    }
    
    /**
     * Get the latest media asset for an entity and type
     * 
     * GET /api/v1/media/entity/{entityId}/type/{type}/latest
     * 
     * @param entityId Entity ID
     * @param type Media type
     * @return MediaAssetResponse or 404 if not found
     */
    @GetMapping("/entity/{entityId}/type/{type}/latest")
    public ResponseEntity<MediaAssetResponse> getLatestMediaByEntityIdAndType(
        @PathVariable UUID entityId,
        @PathVariable MediaType type
    ) {
        log.debug("Get latest media request: entityId={}, type={}", entityId, type);
        
        MediaAssetResponse response = mediaService.getLatestMediaByEntityIdAndType(entityId, type);
        
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Download media file with proper Content-Disposition header
     * Proxies the file from S3 to avoid CORS issues
     * 
     * GET /api/v1/media/{id}/download?size=large
     * 
     * @param id Media asset ID
     * @param size Size variant (large, medium, small, original) - default: large
     * @return File bytes with download headers
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadMedia(
        @PathVariable UUID id,
        @RequestParam(defaultValue = "large") String size
    ) {
        log.info("Download media request: id={}, size={}", id, size);
        
        MediaAssetResponse media = mediaService.getMediaById(id);
        
        // Select S3 key based on size parameter
        String s3Key = mediaService.getS3KeyForDownload(id, size);
        
        // Get file bytes from S3
        byte[] fileBytes = mediaService.downloadFileBytes(s3Key);
        
        // Generate filename
        String filename = "qr-code-" + id + ".png";
        
        // Return file with download headers
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
            .header("Content-Type", media.contentType())
            .body(fileBytes);
    }
    
    /**
     * Download multiple QR codes as a ZIP file
     * Professional bulk download for table/wifi QR codes
     * 
     * POST /api/v1/media/bulk-download
     * 
     * @param mediaIds List of media asset IDs to download
     * @param size Size variant (large, medium, small, original) - default: large
     * @return ZIP file containing all requested QR codes
     */
    @PostMapping("/bulk-download")
    public ResponseEntity<byte[]> bulkDownloadQRCodes(
        @RequestBody List<UUID> mediaIds,
        @RequestParam(defaultValue = "large") String size
    ) {
        log.info("Bulk download request: count={}, size={}", mediaIds.size(), size);
        
        if (mediaIds == null || mediaIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        // Generate ZIP file
        byte[] zipBytes = mediaService.downloadBulkQRCodes(mediaIds, size);
        
        // Generate filename with timestamp
        String timestamp = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String filename = "qr-codes-" + timestamp + ".zip";
        
        // Return ZIP with download headers
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
            .header("Content-Type", "application/zip")
            .body(zipBytes);
    }
    
    /**
     * Delete a media asset (soft delete)
     * 
     * DELETE /api/v1/media/{id}
     * 
     * @param id Media asset ID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteMedia(@PathVariable UUID id) {
        log.info("Delete media request: id={}", id);
        
        mediaService.deleteMedia(id);
        
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Replace existing media for an entity and type
     * 
     * PUT /api/v1/media/entity/{entityId}/type/{type}
     * 
     * @param file New file to upload
     * @param entityId Entity ID
     * @param type Media type
     * @param uploadedBy User ID who uploaded (optional)
     * @return MediaAssetResponse for the new media
     */
    @PutMapping(value = "/entity/{entityId}/type/{type}", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaAssetResponse> replaceMedia(
        @RequestParam("file") MultipartFile file,
        @PathVariable UUID entityId,
        @PathVariable MediaType type,
        @RequestParam(value = "uploadedBy", required = false) UUID uploadedBy
    ) {
        log.info("Replace media request: type={}, entityId={}", type, entityId);
        
        MediaAssetResponse response = mediaService.replaceMedia(file, type, entityId, uploadedBy);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get storage statistics for an entity
     * 
     * GET /api/v1/media/entity/{entityId}/stats
     * 
     * @param entityId Entity ID
     * @return StorageStatsResponse
     */
    @GetMapping("/entity/{entityId}/stats")
    public ResponseEntity<StorageStatsResponse> getStorageStats(@PathVariable UUID entityId) {
        log.debug("Get storage stats request: entityId={}", entityId);
        
        MediaService.StorageStats stats = mediaService.getStorageStats(entityId);
        
        StorageStatsResponse response = StorageStatsResponse.builder()
            .entityId(entityId)
            .fileCount(stats.fileCount())
            .totalBytes(stats.totalBytes())
            .formattedSize(stats.formattedSize())
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Check if media exists for an entity and type
     * 
     * HEAD /api/v1/media/entity/{entityId}/type/{type}
     * 
     * @param entityId Entity ID
     * @param type Media type
     * @return 200 if exists, 404 if not
     */
    @RequestMapping(
        value = "/entity/{entityId}/type/{type}",
        method = RequestMethod.HEAD
    )
    public ResponseEntity<Void> checkMediaExists(
        @PathVariable UUID entityId,
        @PathVariable MediaType type
    ) {
        log.debug("Check media exists request: entityId={}, type={}", entityId, type);
        
        boolean exists = mediaService.mediaExists(entityId, type);
        
        return exists ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
    
    /**
     * Health check endpoint
     * 
     * GET /api/v1/media/health
     * 
     * @return 200 OK
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Media Service is running");
    }
    
    /**
     * Clear Redis cache (for debugging Redis serialization issues)
     * 
     * POST /api/v1/media/cache/clear
     * 
     * @return 200 OK
     */
    @PostMapping("/cache/clear")
    public ResponseEntity<String> clearCache() {
        log.info("Clearing all Redis caches for Media Service");
        
        try {
            // Clear all cache names
            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                    log.info("Cleared cache: {}", cacheName);
                }
            });
            
            return ResponseEntity.ok("All caches cleared successfully");
        } catch (Exception e) {
            log.error("Error clearing caches: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error clearing caches: " + e.getMessage());
        }
    }
}
