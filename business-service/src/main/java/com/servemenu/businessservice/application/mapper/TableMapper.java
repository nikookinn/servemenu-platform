package com.servemenu.businessservice.application.mapper;

import com.servemenu.businessservice.application.dto.response.TableResponse;
import com.servemenu.businessservice.domain.model.Table;
import com.servemenu.businessservice.infrastructure.grpc.client.MediaServiceGrpcClient;
import com.servemenu.mediaservice.grpc.MediaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public abstract class TableMapper {

    @Autowired
    protected MediaServiceGrpcClient mediaServiceGrpcClient;

    @Mapping(target = "storeId", source = "store.id")
    @Mapping(target = "qrCodeUrls", expression = "java(enrichWithQRUrls(table))")
    public abstract TableResponse toResponse(Table table);

    /**
     * Convert list of tables to responses with batch media enrichment (OPTIMIZED!)
     */
    public List<TableResponse> toResponseList(List<Table> tables) {
        if (tables == null || tables.isEmpty()) {
            return List.of();
        }

        // Collect all media IDs for batch fetch
        List<java.util.UUID> mediaIds = tables.stream()
                .map(Table::getQrCodeMediaId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        // Batch fetch all media from Media Service (PERFORMANCE!)
        System.out.println("🔍 [TableMapper] Fetching media for IDs: " + mediaIds);
        java.util.Map<java.util.UUID, MediaResponse> mediaMap = 
                mediaServiceGrpcClient.getMediaByIds(mediaIds);
        System.out.println("🔍 [TableMapper] Media map size: " + mediaMap.size());
        System.out.println("🔍 [TableMapper] Media map keys: " + mediaMap.keySet());

        // Map tables with enriched QR URLs
        return tables.stream()
                .map(table -> toResponseWithMedia(table, mediaMap))
                .toList();
    }

    /**
     * Convert single table with pre-fetched media map
     */
    private TableResponse toResponseWithMedia(Table table, java.util.Map<java.util.UUID, MediaResponse> mediaMap) {
        TableResponse.QRCodeUrls qrUrls = null;
        
        System.out.println("🔍 [TableMapper] Processing table: " + table.getTableName() + ", mediaId: " + table.getQrCodeMediaId());
        
        if (table.getQrCodeMediaId() != null && mediaMap.containsKey(table.getQrCodeMediaId())) {
            MediaResponse media = mediaMap.get(table.getQrCodeMediaId());
            System.out.println("🔍 [TableMapper] Found media for table " + table.getTableName());
            System.out.println("   📦 Media URLs - Large: " + (media.getLargeUrl().isEmpty() ? "EMPTY" : "OK") + 
                             ", Medium: " + (media.getMediumUrl().isEmpty() ? "EMPTY" : "OK") +
                             ", Small: " + (media.getThumbnailUrl().isEmpty() ? "EMPTY" : "OK") +
                             ", Original: " + (media.getOriginalUrl().isEmpty() ? "EMPTY" : "OK"));
            
            String largeUrl = media.getLargeUrl().isEmpty() ? media.getOriginalUrl() : media.getLargeUrl();
            String mediumUrl = media.getMediumUrl().isEmpty() ? media.getThumbnailUrl() : media.getMediumUrl();
            String smallUrl = media.getThumbnailUrl().isEmpty() ? media.getOriginalUrl() : media.getThumbnailUrl();
            
            qrUrls = new TableResponse.QRCodeUrls(
                largeUrl,   // Large: 800x800 - For download
                mediumUrl,  // Medium: 500x500 - For modal preview
                smallUrl    // Small: 200x200 - For quick card preview
            );
            System.out.println("✅ [TableMapper] QR URLs created for " + table.getTableName());
        } else {
            System.out.println("❌ [TableMapper] No media found for table " + table.getTableName() + 
                ", mediaId: " + table.getQrCodeMediaId() + ", mediaMapContains: " + (table.getQrCodeMediaId() != null && mediaMap.containsKey(table.getQrCodeMediaId())));
        }

        return new TableResponse(
                table.getId(),
                table.getStore().getId(),
                table.getTableName(),
                table.getTableNumber(),
                table.getQrCodeMediaId(),
                qrUrls,
                table.getIsActive(),
                table.getCreatedAt(),
                table.getUpdatedAt()
        );
    }

    /**
     * Enrich table with QR URLs from Media Service
     */
    protected TableResponse.QRCodeUrls enrichWithQRUrls(Table table) {
        if (table.getQrCodeMediaId() == null) {
            return null;
        }

        try {
            Optional<MediaResponse> mediaResponse = mediaServiceGrpcClient.getMediaById(table.getQrCodeMediaId());
            
            if (mediaResponse.isPresent()) {
                MediaResponse media = mediaResponse.get();
                String largeUrl = media.getLargeUrl().isEmpty() ? media.getOriginalUrl() : media.getLargeUrl();
                String mediumUrl = media.getMediumUrl().isEmpty() ? media.getThumbnailUrl() : media.getMediumUrl();
                String smallUrl = media.getThumbnailUrl().isEmpty() ? media.getOriginalUrl() : media.getThumbnailUrl();
                
                return new TableResponse.QRCodeUrls(
                    largeUrl,   // Large: 800x800 - For download
                    mediumUrl,  // Medium: 500x500 - For modal preview
                    smallUrl    // Small: 200x200 - For quick card preview
                );
            }
        } catch (Exception e) {
            // Graceful degradation - log but don't fail the request
            // Logger will be injected by MapStruct
        }
        
        return null;
    }
}
