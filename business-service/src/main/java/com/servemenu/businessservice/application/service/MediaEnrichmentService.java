package com.servemenu.businessservice.application.service;

import com.servemenu.businessservice.domain.model.BusinessSettings;
import com.servemenu.businessservice.domain.model.Table;
import com.servemenu.businessservice.domain.model.WifiSettings;
import com.servemenu.businessservice.infrastructure.grpc.client.MediaServiceGrpcClient;
import com.servemenu.mediaservice.grpc.MediaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for enriching entities with media URLs from Media Service
 * 
 * This service fetches media URLs via gRPC and sets them on entity transient fields
 * Uses batch operations for performance optimization
 * 
 * @author ServeMenu Platform Team
 * @version 1.0.0
 * @since 2025-11-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaEnrichmentService {
    
    private final MediaServiceGrpcClient mediaClient;
    
    /**
     * Enrich BusinessSettings with logo and cover image URLs
     * 
     * @param settings BusinessSettings entity to enrich
     */
    public void enrichBusinessSettings(BusinessSettings settings) {
        if (settings == null) {
            return;
        }
        
        List<UUID> mediaIds = new ArrayList<>();
        if (settings.getLogoMediaId() != null) {
            mediaIds.add(settings.getLogoMediaId());
        }
        if (settings.getCoverImageMediaId() != null) {
            mediaIds.add(settings.getCoverImageMediaId());
        }
        
        if (mediaIds.isEmpty()) {
            return;
        }
        
        try {
            Map<UUID, MediaResponse> mediaMap = mediaClient.getMediaByIds(mediaIds);
            
            // Set logo URL (use medium 256x256 for settings page preview)
            if (settings.getLogoMediaId() != null) {
                MediaResponse logo = mediaMap.get(settings.getLogoMediaId());
                if (logo != null) {
                    settings.setLogoUrl(logo.getMediumUrl());
                    log.debug("Enriched logo URL (medium 256x256) for business settings: {}", settings.getId());
                }
            }
            
            // Set cover image URL (use large 1200x675 for settings page preview)
            if (settings.getCoverImageMediaId() != null) {
                MediaResponse cover = mediaMap.get(settings.getCoverImageMediaId());
                if (cover != null) {
                    settings.setCoverImageUrl(cover.getLargeUrl());
                    log.debug("Enriched cover URL (large 1200x675) for business settings: {}", settings.getId());
                }
            }
        } catch (Exception e) {
            log.error("Error enriching business settings {}: {}", settings.getId(), e.getMessage());
            // Graceful degradation - URLs remain null
        }
    }
    
    /**
     * Enrich single Table with QR code URL
     * 
     * @param table Table entity to enrich
     */
    public void enrichTable(Table table) {
        if (table == null || table.getQrCodeMediaId() == null) {
            return;
        }
        
        try {
            mediaClient.getMediaById(table.getQrCodeMediaId())
                    .ifPresent(media -> {
                        table.setQrCodeUrl(media.getLargeUrl());
                        log.debug("Enriched QR code URL for table: {}", table.getId());
                    });
        } catch (Exception e) {
            log.error("Error enriching table {}: {}", table.getId(), e.getMessage());
            // Graceful degradation
        }
    }
    
    /**
     * Enrich multiple Tables with QR code URLs (batch operation - more efficient)
     * 
     * @param tables List of Table entities to enrich
     */
    public void enrichTables(List<Table> tables) {
        if (tables == null || tables.isEmpty()) {
            return;
        }
        
        // Collect all media IDs
        List<UUID> mediaIds = tables.stream()
                .map(Table::getQrCodeMediaId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        
        if (mediaIds.isEmpty()) {
            return;
        }
        
        try {
            // Fetch all media in one batch call
            Map<UUID, MediaResponse> mediaMap = mediaClient.getMediaByIds(mediaIds);
            
            // Enrich each table
            tables.forEach(table -> {
                if (table.getQrCodeMediaId() != null) {
                    MediaResponse media = mediaMap.get(table.getQrCodeMediaId());
                    if (media != null) {
                        table.setQrCodeUrl(media.getLargeUrl());
                    }
                }
            });
            
            log.debug("Enriched {} tables with QR code URLs", tables.size());
        } catch (Exception e) {
            log.error("Error enriching tables batch: {}", e.getMessage());
            // Graceful degradation
        }
    }
    
    /**
     * Enrich WifiSettings with QR code URL
     * 
     * @param wifiSettings WifiSettings entity to enrich
     */
    public void enrichWifiSettings(WifiSettings wifiSettings) {
        if (wifiSettings == null || wifiSettings.getQrCodeMediaId() == null) {
            return;
        }
        
        try {
            mediaClient.getMediaById(wifiSettings.getQrCodeMediaId())
                    .ifPresent(media -> {
                        wifiSettings.setQrCodeUrl(media.getLargeUrl());
                        log.debug("Enriched WiFi QR code URL for store: {}", wifiSettings.getId());
                    });
        } catch (Exception e) {
            log.error("Error enriching wifi settings {}: {}", wifiSettings.getId(), e.getMessage());
            // Graceful degradation
        }
    }
    
    /**
     * Enrich multiple WifiSettings with QR code URLs (batch operation)
     * 
     * @param wifiSettingsList List of WifiSettings entities to enrich
     */
    public void enrichWifiSettingsList(List<WifiSettings> wifiSettingsList) {
        if (wifiSettingsList == null || wifiSettingsList.isEmpty()) {
            return;
        }
        
        List<UUID> mediaIds = wifiSettingsList.stream()
                .map(WifiSettings::getQrCodeMediaId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        
        if (mediaIds.isEmpty()) {
            return;
        }
        
        try {
            Map<UUID, MediaResponse> mediaMap = mediaClient.getMediaByIds(mediaIds);
            
            wifiSettingsList.forEach(wifi -> {
                if (wifi.getQrCodeMediaId() != null) {
                    MediaResponse media = mediaMap.get(wifi.getQrCodeMediaId());
                    if (media != null) {
                        wifi.setQrCodeUrl(media.getLargeUrl());
                    }
                }
            });
            
            log.debug("Enriched {} wifi settings with QR code URLs", wifiSettingsList.size());
        } catch (Exception e) {
            log.error("Error enriching wifi settings batch: {}", e.getMessage());
        }
    }
    
    /**
     * Enrich QRCustomization with logo URL
     * 
     * @param qrCustomization QRCustomization entity to enrich
     */
    public void enrichQRCustomization(com.servemenu.businessservice.domain.model.QRCustomization qrCustomization) {
        if (qrCustomization == null || qrCustomization.getLogoMediaId() == null) {
            return;
        }
        
        try {
            mediaClient.getMediaById(qrCustomization.getLogoMediaId())
                    .ifPresent(media -> {
                        // Use medium size (256x256) for QR logo preview in frontend
                        qrCustomization.setLogoUrl(media.getMediumUrl());
                        log.debug("Enriched QR logo URL for customization: {}", qrCustomization.getId());
                    });
        } catch (Exception e) {
            log.error("Error enriching QR customization {}: {}", qrCustomization.getId(), e.getMessage());
            // Graceful degradation - logo URL remains null
        }
    }
    
    /**
     * Check if media exists (for validation)
     * 
     * @param mediaId Media ID to check
     * @return true if exists, false otherwise
     */
    public boolean mediaExists(UUID mediaId) {
        if (mediaId == null) {
            return false;
        }
        return mediaClient.mediaExists(mediaId);
    }
    
    /**
     * Delete media from Media Service (soft delete via gRPC)
     * Used when logo is changed or removed from QR customization
     * 
     * @param mediaId Media ID to delete
     */
    public void deleteMedia(UUID mediaId) {
        if (mediaId == null) {
            log.warn("Attempted to delete null mediaId");
            return;
        }
        
        try {
            log.info("🗑️ Deleting media via gRPC: mediaId={}", mediaId);
            
            boolean deleted = mediaClient.deleteMedia(mediaId);
            
            if (deleted) {
                log.info("✅ Media deleted successfully via gRPC: mediaId={}", mediaId);
            } else {
                log.warn("⚠️ Media deletion failed or media not found: mediaId={}", mediaId);
            }
        } catch (Exception e) {
            log.error("❌ Error deleting media {}: {}", mediaId, e.getMessage(), e);
            // Non-critical - continue even if delete fails
            // The media will remain in S3 but marked as deleted in DB
        }
    }
}
