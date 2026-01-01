package com.servemenu.mediaservice.domain.repository;

import com.servemenu.mediaservice.domain.enums.MediaType;
import com.servemenu.mediaservice.domain.model.MediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for MediaAsset entity.
 * Provides database access methods with soft delete support.
 */
@Repository
public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {
    
    /**
     * Find an active (non-deleted) media asset by ID
     * @param id Media asset ID
     * @return Optional containing the media asset if found and active
     */
    @Query("SELECT m FROM MediaAsset m WHERE m.id = :id AND m.deletedAt IS NULL")
    Optional<MediaAsset> findByIdAndActive(@Param("id") UUID id);
    
    /**
     * Find an active media asset by S3 key
     * @param s3Key S3 object key
     * @return Optional containing the media asset if found and active
     */
    @Query("SELECT m FROM MediaAsset m WHERE m.s3Key = :s3Key AND m.deletedAt IS NULL")
    Optional<MediaAsset> findByS3KeyAndActive(@Param("s3Key") String s3Key);
    
    /**
     * Find all active media assets for a specific entity
     * @param entityId Entity ID
     * @return List of active media assets
     */
    @Query("SELECT m FROM MediaAsset m WHERE m.entityId = :entityId AND m.deletedAt IS NULL ORDER BY m.createdAt DESC")
    List<MediaAsset> findByEntityIdAndActive(@Param("entityId") UUID entityId);
    
    /**
     * Find all active media assets for a specific entity and type
     * @param entityId Entity ID
     * @param mediaType Media type
     * @return List of active media assets
     */
    @Query("SELECT m FROM MediaAsset m WHERE m.entityId = :entityId AND m.mediaType = :mediaType AND m.deletedAt IS NULL ORDER BY m.createdAt DESC")
    List<MediaAsset> findByEntityIdAndMediaTypeAndActive(
        @Param("entityId") UUID entityId,
        @Param("mediaType") MediaType mediaType
    );
    
    /**
     * Find the most recent active media asset for an entity and type
     * @param entityId Entity ID
     * @param mediaType Media type
     * @return Optional containing the most recent media asset
     */
    @Query("SELECT m FROM MediaAsset m WHERE m.entityId = :entityId AND m.mediaType = :mediaType AND m.deletedAt IS NULL ORDER BY m.createdAt DESC LIMIT 1")
    Optional<MediaAsset> findLatestByEntityIdAndMediaType(
        @Param("entityId") UUID entityId,
        @Param("mediaType") MediaType mediaType
    );
    
    /**
     * Find all media assets uploaded by a specific user
     * @param uploadedBy User ID
     * @return List of active media assets
     */
    @Query("SELECT m FROM MediaAsset m WHERE m.uploadedBy = :uploadedBy AND m.deletedAt IS NULL ORDER BY m.createdAt DESC")
    List<MediaAsset> findByUploadedByAndActive(@Param("uploadedBy") UUID uploadedBy);
    
    /**
     * Count active media assets for an entity
     * @param entityId Entity ID
     * @return Count of active media assets
     */
    @Query("SELECT COUNT(m) FROM MediaAsset m WHERE m.entityId = :entityId AND m.deletedAt IS NULL")
    long countByEntityIdAndActive(@Param("entityId") UUID entityId);
    
    /**
     * Count active media assets by type
     * @param mediaType Media type
     * @return Count of active media assets
     */
    @Query("SELECT COUNT(m) FROM MediaAsset m WHERE m.mediaType = :mediaType AND m.deletedAt IS NULL")
    long countByMediaTypeAndActive(@Param("mediaType") MediaType mediaType);
    
    /**
     * Find all soft-deleted media assets older than the specified date
     * Used for cleanup jobs
     * @param deletedBefore Cutoff date
     * @return List of old deleted media assets
     */
    @Query("SELECT m FROM MediaAsset m WHERE m.deletedAt IS NOT NULL AND m.deletedAt < :deletedBefore")
    List<MediaAsset> findDeletedBefore(@Param("deletedBefore") Instant deletedBefore);
    
    /**
     * Calculate total storage used by active media assets
     * @return Total file size in bytes
     */
    @Query("SELECT COALESCE(SUM(m.fileSize), 0) FROM MediaAsset m WHERE m.deletedAt IS NULL")
    long calculateTotalStorageUsed();
    
    /**
     * Calculate total storage used by a specific entity
     * @param entityId Entity ID
     * @return Total file size in bytes
     */
    @Query("SELECT COALESCE(SUM(m.fileSize), 0) FROM MediaAsset m WHERE m.entityId = :entityId AND m.deletedAt IS NULL")
    long calculateStorageUsedByEntity(@Param("entityId") UUID entityId);
    
    /**
     * Check if a media asset exists for an entity and type
     * @param entityId Entity ID
     * @param mediaType Media type
     * @return true if exists
     */
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM MediaAsset m WHERE m.entityId = :entityId AND m.mediaType = :mediaType AND m.deletedAt IS NULL")
    boolean existsByEntityIdAndMediaTypeAndActive(
        @Param("entityId") UUID entityId,
        @Param("mediaType") MediaType mediaType
    );
    
    // ==================== gRPC Support Methods ====================
    
    /**
     * Find active media asset by ID (for gRPC)
     * Uses deletedAt IS NULL check
     */
    @Query("SELECT m FROM MediaAsset m WHERE m.id = :id AND m.deletedAt IS NULL")
    Optional<MediaAsset> findByIdAndDeletedFalse(@Param("id") UUID id);
    
    /**
     * Find multiple active media assets by IDs (for gRPC batch operations)
     */
    @Query("SELECT m FROM MediaAsset m WHERE m.id IN :ids AND m.deletedAt IS NULL")
    List<MediaAsset> findAllByIdInAndDeletedFalse(@Param("ids") List<UUID> ids);
    
    /**
     * Find all active media assets by entity ID (for gRPC)
     */
    @Query("SELECT m FROM MediaAsset m WHERE m.entityId = :entityId AND m.deletedAt IS NULL ORDER BY m.createdAt DESC")
    List<MediaAsset> findByEntityIdAndDeletedFalse(@Param("entityId") UUID entityId);
    
    /**
     * Find active media assets by entity ID and type (for gRPC)
     * Note: type is stored as string in MediaType enum
     */
    @Query("SELECT m FROM MediaAsset m WHERE m.entityId = :entityId AND m.mediaType = :type AND m.deletedAt IS NULL ORDER BY m.createdAt DESC")
    List<MediaAsset> findByEntityIdAndTypeAndDeletedFalse(
        @Param("entityId") UUID entityId,
        @Param("type") String type
    );
    
    /**
     * Check if media exists by ID (for gRPC)
     */
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM MediaAsset m WHERE m.id = :id AND m.deletedAt IS NULL")
    boolean existsByIdAndDeletedFalse(@Param("id") UUID id);
}
