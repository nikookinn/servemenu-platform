package com.servemenu.mediaservice.application.service;

import com.servemenu.mediaservice.application.dto.response.MediaAssetResponse;
import com.servemenu.mediaservice.application.dto.response.MediaAssetResponseWrapper;
import com.servemenu.mediaservice.application.dto.response.MediaAssetListResponseWrapper;
import com.servemenu.mediaservice.config.RedisConfig;
import com.servemenu.mediaservice.domain.enums.MediaType;
import com.servemenu.mediaservice.domain.model.MediaAsset;
import com.servemenu.mediaservice.domain.repository.MediaAssetRepository;
import com.servemenu.mediaservice.infrastructure.exception.MediaNotFoundException;
import com.servemenu.mediaservice.infrastructure.storage.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Main service for media asset management.
 * Orchestrates validation, storage, and metadata operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaService {
    
    private final MediaAssetRepository mediaAssetRepository;
    private final S3StorageService s3StorageService;
    private final ValidationService validationService;
    private final MetadataService metadataService;
    private final ImageProcessingService imageProcessingService;
    private final com.servemenu.mediaservice.infrastructure.storage.S3UploadHelper s3UploadHelper;
    
    /**
     * Upload a media file
     * CACHE EVICTION: Clears entity cache on new upload
     * 
     * @param file MultipartFile to upload
     * @param mediaType Type of media
     * @param entityId Entity ID this media belongs to
     * @param uploadedBy User ID who uploaded (optional)
     * @return MediaAssetResponse containing upload details
     */
    @CacheEvict(value = {RedisConfig.MEDIA_BY_ENTITY, RedisConfig.MEDIA_BY_ID}, key = "#entityId")
    @Transactional
    public MediaAssetResponse uploadMedia(
        MultipartFile file,
        MediaType mediaType,
        UUID entityId,
        UUID uploadedBy
    ) {
        log.info("Starting media upload: type={}, entityId={}, filename={}", 
            mediaType, entityId, file.getOriginalFilename());
        
        // Step 1: Validate the file
        validationService.validateMediaFile(file, mediaType);
        
        // Step 2: Extract metadata
        MetadataService.ImageDimensions dimensions;
        try {
            dimensions = metadataService.extractImageDimensions(file);
        } catch (IOException e) {
            log.error("Failed to extract image dimensions: {}", e.getMessage(), e);
            dimensions = new MetadataService.ImageDimensions(null, null);
        }
        
        // Step 3: Upload original to S3
        String s3Key = s3StorageService.uploadFile(file, entityId, mediaType);
        String s3Url = s3StorageService.generatePublicUrl(s3Key);
        
        // Step 4: Generate and upload resized images (if image)
        String thumbnailS3Key = null;
        String mediumS3Key = null;
        String largeS3Key = null;
        
        if (imageProcessingService.isImage(file.getContentType())) {
            try {
                String extension = imageProcessingService.getExtensionFromContentType(file.getContentType());
                
                // Generate sizes based on media type
                switch (mediaType) {
                    case BUSINESS_QR, TABLE_QR, WIFI_QR -> {
                        // QR Codes: thumbnail=200x200 (quick preview), medium=500x500 (modal), large=800x800 (download)
                        // Use HIGH_QUALITY (90%) for QR codes - must be sharp for scanning
                        thumbnailS3Key = generateAndUpload(file, entityId, mediaType, 
                            ImageProcessingService.QR_SMALL_SIZE, 
                            ImageProcessingService.QR_SMALL_SIZE, 
                            "thumbnails", extension, ImageProcessingService.HIGH_QUALITY);
                        
                        mediumS3Key = generateAndUpload(file, entityId, mediaType, 
                            ImageProcessingService.QR_MEDIUM_SIZE, 
                            ImageProcessingService.QR_MEDIUM_SIZE, 
                            "medium", extension, ImageProcessingService.HIGH_QUALITY);
                        
                        largeS3Key = generateAndUpload(file, entityId, mediaType, 
                            ImageProcessingService.QR_LARGE_SIZE, 
                            ImageProcessingService.QR_LARGE_SIZE, 
                            "large", extension, ImageProcessingService.HIGH_QUALITY);
                    }
                    case QR_LOGO -> {
                        // QR Logo: Only medium size (512x512) - used for QR embedding
                        // Higher resolution for better quality when embedded in QR codes
                        // Use HIGH_QUALITY (90%) for QR logos - needs to be sharp when embedded
                        mediumS3Key = generateAndUpload(file, entityId, mediaType, 
                            512, 512, "medium", extension, ImageProcessingService.HIGH_QUALITY);
                    }
                    case MENU_ITEM -> {
                        // Items: thumbnail=200x200, medium=400x400, large=800x800
                        // Use MEDIUM_QUALITY (80%) for food images - balance quality/size
                        thumbnailS3Key = generateAndUpload(file, entityId, mediaType, 
                            ImageProcessingService.ITEM_THUMBNAIL_SIZE, 
                            ImageProcessingService.ITEM_THUMBNAIL_SIZE, 
                            "thumbnails", extension, ImageProcessingService.THUMBNAIL_QUALITY);
                        
                        mediumS3Key = generateAndUpload(file, entityId, mediaType, 
                            ImageProcessingService.ITEM_MEDIUM_SIZE, 
                            ImageProcessingService.ITEM_MEDIUM_SIZE, 
                            "medium", extension, ImageProcessingService.MEDIUM_QUALITY);
                        
                        largeS3Key = generateAndUpload(file, entityId, mediaType, 
                            ImageProcessingService.ITEM_LARGE_SIZE, 
                            ImageProcessingService.ITEM_LARGE_SIZE, 
                            "large", extension, ImageProcessingService.MEDIUM_QUALITY);
                    }
                    case LOGO -> {
                        // Logo: thumbnail=64x64, medium=256x256 (1:1 square)
                        // Optimized for navbar (40-60px) and profile (80-100px)
                        // Use WebP format for 30% smaller files
                        thumbnailS3Key = generateAndUpload(file, entityId, mediaType, 
                            64, 64, "thumbnails", extension, ImageProcessingService.THUMBNAIL_QUALITY, "webp");
                        
                        mediumS3Key = generateAndUpload(file, entityId, mediaType, 
                            256, 256, "medium", extension, ImageProcessingService.HIGH_QUALITY, "webp");
                    }
                    case COVER_IMAGE -> {
                        // Cover: thumbnail=320x180, medium=800x450, large=1200x675 (16:9)
                        // Optimized for mobile (320-768px) and desktop (1200-1440px)
                        // Use WebP format for 30% smaller files
                        thumbnailS3Key = generateAndUpload(file, entityId, mediaType, 
                            320, 180, "thumbnails", extension, ImageProcessingService.THUMBNAIL_QUALITY, "webp");
                        
                        mediumS3Key = generateAndUpload(file, entityId, mediaType, 
                            800, 450, "medium", extension, ImageProcessingService.MEDIUM_QUALITY, "webp");
                        
                        largeS3Key = generateAndUpload(file, entityId, mediaType, 
                            1200, 675, "large", extension, ImageProcessingService.MEDIUM_QUALITY, "webp");
                    }
                }
                
                log.info("Generated and uploaded resized images for: {}", s3Key);
                
            } catch (IOException e) {
                log.error("Failed to generate resized images: {}", e.getMessage(), e);
                // Continue without resized images (graceful degradation)
            }
        }
        
        // Step 5: Save metadata to database
        MediaAsset mediaAsset = MediaAsset.builder()
            .originalFilename(file.getOriginalFilename())
            .s3Key(s3Key)
            .thumbnailS3Key(thumbnailS3Key)
            .mediumS3Key(mediumS3Key)
            .largeS3Key(largeS3Key)
            .s3Url(s3Url)
            .mediaType(mediaType)
            .contentType(file.getContentType())
            .fileSize(file.getSize())
            .width(dimensions.width())
            .height(dimensions.height())
            .entityId(entityId)
            .uploadedBy(uploadedBy)
            .build();
        
        MediaAsset savedAsset = mediaAssetRepository.save(mediaAsset);
        
        log.info("Media upload completed: id={}, s3Key={}", savedAsset.getId(), s3Key);
        
        return mapToResponse(savedAsset);
    }
    
    /**
     * Get media asset by ID
     * CACHED: 1 hour TTL (frequently accessed, rarely changes)
     * Uses wrapper for Redis serialization compatibility
     * 
     * @param id Media asset ID
     * @return MediaAssetResponse
     * @throws MediaNotFoundException if not found
     */
    @Cacheable(value = RedisConfig.MEDIA_BY_ID, key = "#id", unless = "#result == null")
    @Transactional(readOnly = true)
    public MediaAssetResponseWrapper getMediaByIdCached(UUID id) {
        log.debug("Fetching media asset from DB: id={}", id);
        
        MediaAsset mediaAsset = mediaAssetRepository.findByIdAndActive(id)
            .orElseThrow(() -> new MediaNotFoundException(id));
        
        MediaAssetResponse response = mapToResponse(mediaAsset);
        return MediaAssetResponseWrapper.of(response);
    }
    
    /**
     * Public API method that uses cached wrapper internally
     */
    @Transactional(readOnly = true)
    public MediaAssetResponse getMediaById(UUID id) {
        MediaAssetResponseWrapper wrapper = getMediaByIdCached(id);
        return wrapper.toResponse();
    }
    
    /**
     * Get all media assets for an entity
     * CACHED: 30 minutes TTL (moderately accessed)
     * Uses wrapper for Redis serialization compatibility
     * 
     * @param entityId Entity ID
     * @return MediaAssetListResponseWrapper
     */
    @Cacheable(value = RedisConfig.MEDIA_BY_ENTITY, key = "#entityId", unless = "#result == null || #result.mediaAssets.isEmpty()")
    @Transactional(readOnly = true)
    public MediaAssetListResponseWrapper getMediaByEntityIdCached(UUID entityId) {
        log.debug("Fetching media assets from DB for entity: entityId={}", entityId);
        
        List<MediaAsset> mediaAssets = mediaAssetRepository.findByEntityIdAndActive(entityId);
        
        List<MediaAssetResponse> responses = mediaAssets.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
        
        return MediaAssetListResponseWrapper.of(responses);
    }
    
    /**
     * Public API method that uses cached wrapper internally
     */
    @Transactional(readOnly = true)
    public List<MediaAssetResponse> getMediaByEntityId(UUID entityId) {
        MediaAssetListResponseWrapper wrapper = getMediaByEntityIdCached(entityId);
        return wrapper.toResponseList();
    }
    
    /**
     * Get media assets by entity ID and type
     * 
     * @param entityId Entity ID
     * @param mediaType Media type
     * @return List of MediaAssetResponse
     */
    @Transactional(readOnly = true)
    public List<MediaAssetResponse> getMediaByEntityIdAndType(UUID entityId, MediaType mediaType) {
        log.debug("Fetching media assets: entityId={}, type={}", entityId, mediaType);
        
        List<MediaAsset> mediaAssets = mediaAssetRepository
            .findByEntityIdAndMediaTypeAndActive(entityId, mediaType);
        
        return mediaAssets.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get the latest media asset for an entity and type
     * 
     * @param entityId Entity ID
     * @param mediaType Media type
     * @return MediaAssetResponse or null if not found
     */
    @Transactional(readOnly = true)
    public MediaAssetResponse getLatestMediaByEntityIdAndType(UUID entityId, MediaType mediaType) {
        log.debug("Fetching latest media asset: entityId={}, type={}", entityId, mediaType);
        
        return mediaAssetRepository.findLatestByEntityIdAndMediaType(entityId, mediaType)
            .map(this::mapToResponse)
            .orElse(null);
    }
    
    /**
     * Delete a media asset (soft delete)
     * CACHE EVICTION: Removes from cache on delete
     * 
     * @param id Media asset ID
     * @throws MediaNotFoundException if not found
     */
    @CacheEvict(value = {RedisConfig.MEDIA_BY_ID, RedisConfig.MEDIA_BY_ENTITY}, allEntries = true)
    @Transactional
    public void deleteMedia(UUID id) {
        log.info("Deleting media asset: id={}", id);
        
        MediaAsset mediaAsset = mediaAssetRepository.findByIdAndActive(id)
            .orElseThrow(() -> new MediaNotFoundException(id));
        
        // Soft delete in database
        mediaAsset.softDelete();
        mediaAssetRepository.save(mediaAsset);
        
        log.info("Media asset soft deleted and cache evicted: id={}, s3Key={}", id, mediaAsset.getS3Key());
        
        // Note: Physical S3 deletion will be handled by a scheduled cleanup job
        // This prevents accidental data loss and allows for recovery
    }
    
    /**
     * Batch delete multiple media assets (for store deletion)
     * Professional approach: Single database query + batch S3 deletion
     * 
     * @param ids List of media asset IDs
     * @return Number of successfully deleted media assets
     */
    @CacheEvict(value = {RedisConfig.MEDIA_BY_ID, RedisConfig.MEDIA_BY_ENTITY}, allEntries = true)
    @Transactional
    public int batchDeleteMedia(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            log.info("No media assets to delete");
            return 0;
        }
        
        log.info("Batch deleting {} media assets", ids.size());
        
        // Fetch all media assets in one query
        List<MediaAsset> mediaAssets = mediaAssetRepository.findAllById(ids);
        
        if (mediaAssets.isEmpty()) {
            log.warn("No media assets found for batch deletion: ids={}", ids);
            return 0;
        }
        
        int deletedCount = 0;
        
        // Delete from S3 (batch operation)
        for (MediaAsset mediaAsset : mediaAssets) {
            try {
                s3StorageService.deleteFile(mediaAsset.getS3Key());
                deletedCount++;
                log.debug("Deleted from S3: s3Key={}", mediaAsset.getS3Key());
            } catch (Exception e) {
                log.error("Failed to delete file from S3: s3Key={}, error={}", 
                        mediaAsset.getS3Key(), e.getMessage());
                // Continue with other deletions
            }
        }
        
        // Delete from database (batch operation)
        mediaAssetRepository.deleteAll(mediaAssets);
        
        log.info("Batch deleted {} media assets successfully (S3: {}, DB: {})", 
                mediaAssets.size(), deletedCount, mediaAssets.size());
        
        return deletedCount;
    }
    
    /**
     * Permanently delete a media asset from both database and S3
     * Should only be called by cleanup jobs
     * 
     * @param id Media asset ID
     */
    @Transactional
    public void permanentlyDeleteMedia(UUID id) {
        log.info("Permanently deleting media asset: id={}", id);
        
        MediaAsset mediaAsset = mediaAssetRepository.findById(id)
            .orElseThrow(() -> new MediaNotFoundException(id));
        
        // Delete from S3
        try {
            s3StorageService.deleteFile(mediaAsset.getS3Key());
        } catch (Exception e) {
            log.error("Failed to delete file from S3: {}", mediaAsset.getS3Key(), e);
            // Continue with database deletion even if S3 deletion fails
        }
        
        // Delete from database
        mediaAssetRepository.delete(mediaAsset);
        
        log.info("Media asset permanently deleted: id={}", id);
    }
    
    /**
     * Replace existing media for an entity and type
     * Soft deletes the old media and uploads the new one
     * 
     * @param file New file to upload
     * @param mediaType Media type
     * @param entityId Entity ID
     * @param uploadedBy User ID
     * @return MediaAssetResponse for the new media
     */
    @Transactional
    public MediaAssetResponse replaceMedia(
        MultipartFile file,
        MediaType mediaType,
        UUID entityId,
        UUID uploadedBy
    ) {
        log.info("Replacing media: type={}, entityId={}", mediaType, entityId);
        
        // Find existing media
        mediaAssetRepository.findLatestByEntityIdAndMediaType(entityId, mediaType)
            .ifPresent(existingMedia -> {
                log.info("Soft deleting existing media: id={}", existingMedia.getId());
                existingMedia.softDelete();
                mediaAssetRepository.save(existingMedia);
            });
        
        // Upload new media
        return uploadMedia(file, mediaType, entityId, uploadedBy);
    }
    
    /**
     * Get storage statistics for an entity
     * 
     * @param entityId Entity ID
     * @return Storage statistics
     */
    @Transactional(readOnly = true)
    public StorageStats getStorageStats(UUID entityId) {
        long count = mediaAssetRepository.countByEntityIdAndActive(entityId);
        long totalSize = mediaAssetRepository.calculateStorageUsedByEntity(entityId);
        
        return new StorageStats(count, totalSize, metadataService.formatFileSize(totalSize));
    }
    
    /**
     * Check if media exists for an entity and type
     * 
     * @param entityId Entity ID
     * @param mediaType Media type
     * @return true if exists
     */
    @Transactional(readOnly = true)
    public boolean mediaExists(UUID entityId, MediaType mediaType) {
        return mediaAssetRepository.existsByEntityIdAndMediaTypeAndActive(entityId, mediaType);
    }
    
    /**
     * Helper method to generate and upload resized image
     */
    private String generateAndUpload(
            MultipartFile file,
            UUID entityId,
            MediaType mediaType,
            int width,
            int height,
            String sizeType,
            String extension,
            float quality
    ) throws IOException {
        // Default to JPEG format
        return generateAndUpload(file, entityId, mediaType, width, height, sizeType, extension, quality, "jpg");
    }
    
    /**
     * Helper method to generate and upload resized image with custom format
     */
    private String generateAndUpload(
            MultipartFile file,
            UUID entityId,
            MediaType mediaType,
            int width,
            int height,
            String sizeType,
            String extension,
            float quality,
            String format
    ) throws IOException {
        byte[] resizedBytes = imageProcessingService.resizeImage(file, width, height, quality, format);
        
        // Override extension with actual output format
        String actualExtension = format.equals("webp") ? "webp" : extension;
        
        return s3UploadHelper.uploadResizedImage(
            resizedBytes, 
            entityId, 
            mediaType, 
            sizeType, 
            actualExtension, 
            file.getContentType()
        );
    }
    
    /**
     * Get S3 key for download based on size parameter
     * 
     * @param mediaId Media asset ID
     * @param size Size variant (large, medium, small, original)
     * @return S3 key for the requested size
     */
    public String getS3KeyForDownload(UUID mediaId, String size) {
        MediaAsset mediaAsset = mediaAssetRepository.findByIdAndActive(mediaId)
            .orElseThrow(() -> new MediaNotFoundException(mediaId));
        
        return switch (size.toLowerCase()) {
            case "small", "thumbnail" -> mediaAsset.getThumbnailS3Key() != null 
                ? mediaAsset.getThumbnailS3Key() 
                : mediaAsset.getS3Key();
            case "medium" -> mediaAsset.getMediumS3Key() != null 
                ? mediaAsset.getMediumS3Key() 
                : (mediaAsset.getLargeS3Key() != null ? mediaAsset.getLargeS3Key() : mediaAsset.getS3Key());
            case "original" -> mediaAsset.getS3Key();
            default -> mediaAsset.getLargeS3Key() != null 
                ? mediaAsset.getLargeS3Key() 
                : mediaAsset.getS3Key(); // large is default
        };
    }
    
    /**
     * Download file bytes from S3
     * 
     * @param s3Key S3 object key
     * @return File bytes
     */
    public byte[] downloadFileBytes(String s3Key) {
        return s3StorageService.downloadFile(s3Key);
    }
    
    /**
     * Map MediaAsset entity to response DTO
     * Uses pre-signed URLs for secure temporary access (15 min expiration)
     */
    private MediaAssetResponse mapToResponse(MediaAsset mediaAsset) {
        // Generate pre-signed URLs (secure, temporary access)
        String originalUrl = s3StorageService.generatePresignedUrl(mediaAsset.getS3Key());
        
        String thumbnailUrl = mediaAsset.getThumbnailS3Key() != null 
            ? s3StorageService.generatePresignedUrl(mediaAsset.getThumbnailS3Key())
            : originalUrl;  // Fallback to original
        
        String mediumUrl = mediaAsset.getMediumS3Key() != null 
            ? s3StorageService.generatePresignedUrl(mediaAsset.getMediumS3Key())
            : thumbnailUrl;  // Fallback to thumbnail (for backward compatibility)
        
        // For largeUrl, generate download URL for QR codes (forces download instead of display)
        String largeUrl;
        if (mediaAsset.getLargeS3Key() != null) {
            if (mediaAsset.getMediaType() == MediaType.TABLE_QR || mediaAsset.getMediaType() == MediaType.WIFI_QR) {
                // QR codes: Generate download URL with Content-Disposition header
                String filename = "qr-code-" + mediaAsset.getId() + ".png";
                largeUrl = s3StorageService.generatePresignedDownloadUrl(mediaAsset.getLargeS3Key(), filename);
            } else {
                // Other media: Normal presigned URL
                largeUrl = s3StorageService.generatePresignedUrl(mediaAsset.getLargeS3Key());
            }
        } else {
            largeUrl = originalUrl;  // Fallback to original
        }
        
        return MediaAssetResponse.builder()
            .id(mediaAsset.getId())
            .originalFilename(mediaAsset.getOriginalFilename())
            .thumbnailUrl(thumbnailUrl)
            .mediumUrl(mediumUrl)
            .largeUrl(largeUrl)
            .originalUrl(originalUrl)
            .mediaType(mediaAsset.getMediaType())
            .contentType(mediaAsset.getContentType())
            .fileSize(mediaAsset.getFileSize())
            .formattedFileSize(mediaAsset.getFormattedFileSize())
            .width(mediaAsset.getWidth())
            .height(mediaAsset.getHeight())
            .entityId(mediaAsset.getEntityId())
            .uploadedBy(mediaAsset.getUploadedBy())
            .createdAt(mediaAsset.getCreatedAt())
            .build();
    }
    
    /**
     * Download multiple QR codes as a ZIP file
     * Professional bulk download implementation for table QR codes
     * Uses ORIGINAL size for high-quality printing
     * Filenames are table names for easy identification
     * 
     * @param mediaIds List of media asset IDs to download
     * @param size Size variant (large, medium, small, original) - default: original for printing
     * @return ZIP file bytes containing all QR codes with table names
     */
    public byte[] downloadBulkQRCodes(List<UUID> mediaIds, String size) {
        log.info("Starting bulk QR download: count={}, size={}", mediaIds.size(), size);
        
        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
             java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos)) {
            
            int successCount = 0;
            int failureCount = 0;
            
            // Track filenames to prevent duplicates in ZIP
            java.util.Set<String> usedFilenames = new java.util.HashSet<>();
            
            for (UUID mediaId : mediaIds) {
                try {
                    // Get media asset
                    MediaAsset mediaAsset = mediaAssetRepository.findByIdAndActive(mediaId)
                        .orElseThrow(() -> new MediaNotFoundException(mediaId));
                    
                    // ALWAYS use ORIGINAL size for printing (highest quality)
                    // Original QR codes are generated at optimal resolution for printing
                    String s3Key = mediaAsset.getS3Key(); // Original is always best for print
                    
                    // Download file from S3
                    byte[] fileBytes = s3StorageService.downloadFile(s3Key);
                    
                    // Use original filename (table name) for easy identification
                    // Format: "Table-A1.png", "Table-B2.png", etc.
                    String baseFilename = mediaAsset.getOriginalFilename();
                    if (baseFilename == null || baseFilename.isEmpty()) {
                        // Fallback: generate filename with ID
                        baseFilename = "QR-Code-" + mediaAsset.getId() + ".png";
                    }
                    
                    // Ensure filename has .png extension
                    if (!baseFilename.toLowerCase().endsWith(".png")) {
                        baseFilename = baseFilename + ".png";
                    }
                    
                    // Make filename unique if duplicate exists
                    String filename = baseFilename;
                    int counter = 1;
                    while (usedFilenames.contains(filename)) {
                        // Extract name and extension
                        String nameWithoutExt = baseFilename.substring(0, baseFilename.lastIndexOf(".png"));
                        filename = String.format("%s-(%d).png", nameWithoutExt, counter);
                        counter++;
                    }
                    usedFilenames.add(filename);
                    
                    // Add to ZIP
                    java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(filename);
                    zos.putNextEntry(zipEntry);
                    zos.write(fileBytes);
                    zos.closeEntry();
                    
                    successCount++;
                    log.debug("Added to ZIP: {} ({} bytes)", filename, fileBytes.length);
                    
                } catch (Exception e) {
                    failureCount++;
                    log.error("Failed to add QR code to ZIP: mediaId={}, error={}", mediaId, e.getMessage());
                    // Continue with next file instead of failing entire operation
                }
            }
            
            zos.finish();
            log.info("Bulk QR download completed: success={}, failures={}, totalSize={}KB", 
                successCount, failureCount, baos.size() / 1024);
            
            return baos.toByteArray();
            
        } catch (IOException e) {
            log.error("Failed to create ZIP file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create ZIP file", e);
        }
    }
    
    /**
     * Storage statistics record
     */
    public record StorageStats(long fileCount, long totalBytes, String formattedSize) {}
}
