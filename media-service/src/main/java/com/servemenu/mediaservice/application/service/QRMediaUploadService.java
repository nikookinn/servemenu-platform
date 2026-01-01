package com.servemenu.mediaservice.application.service;

import com.servemenu.mediaservice.domain.enums.MediaType;
import com.servemenu.mediaservice.domain.event.MediaUploadedEvent;
import com.servemenu.shared.events.avro.QRGeneratedEvent;
import com.servemenu.mediaservice.domain.model.MediaAsset;
import com.servemenu.mediaservice.domain.repository.MediaAssetRepository;
import com.servemenu.mediaservice.infrastructure.kafka.producer.DomainEventPublisher;
import com.servemenu.mediaservice.infrastructure.storage.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class QRMediaUploadService {

    private final S3StorageService s3StorageService;
    private final MediaAssetRepository mediaAssetRepository;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional
    public void processQRGenerated(QRGeneratedEvent event) {
        try {
            String qrType = event.getQrType().toString();
            log.info("🎨 Processing QR image upload: qrType={}, storeId={}", qrType, event.getStoreId());

            // Decode base64 image - remove data:image/png;base64, prefix if present
            String base64Data = event.getQrImageBase64().toString();
            if (base64Data.contains(",")) {
                base64Data = base64Data.split(",")[1];
            }
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);

            // Determine MediaType and entityId based on QR type
            MediaType mediaType = mapQRTypeToMediaType(qrType);
            String entityId = getEntityId(event, qrType);
            
            log.info("📋 QR details: qrType={}, mediaType={}, entityId={}", qrType, mediaType, entityId);

            // Upload original to S3 with proper path
            String storeIdOrNull = event.getStoreId() != null ? event.getStoreId().toString() : "null";
            String originalKey = String.format("%s/%s/%s/original.png", 
                    mediaType.getS3Prefix(), storeIdOrNull, entityId);
            String originalUrl = s3StorageService.uploadBytes(originalKey, imageBytes, "image/png");

            log.info("✅ QR image uploaded to S3: {}", originalUrl);

            // Generate thumbnails using Thumbnailator
            byte[] largeBytes = resizeImage(imageBytes, 800, 800);
            byte[] thumbnailBytes = resizeImage(imageBytes, 200, 200);

            String largeKey = String.format("%s/%s/%s/large.png", 
                    mediaType.getS3Prefix(), storeIdOrNull, entityId);
            String thumbnailKey = String.format("%s/%s/%s/thumbnail.png", 
                    mediaType.getS3Prefix(), storeIdOrNull, entityId);

            String largeUrl = s3StorageService.uploadBytes(largeKey, largeBytes, "image/png");
            String thumbnailUrl = s3StorageService.uploadBytes(thumbnailKey, thumbnailBytes, "image/png");

            // Create MediaAsset
            // Use displayName from event for filename (table name, WiFi SSID, or business name)
            String filename = event.getDisplayName() != null && !event.getDisplayName().toString().isEmpty()
                    ? event.getDisplayName().toString() + ".png"
                    : "qr-code.png"; // Fallback
            
            MediaAsset mediaAsset = MediaAsset.builder()
                    .originalFilename(filename)
                    .s3Key(originalKey)
                    .s3Url(originalUrl)
                    .thumbnailS3Key(thumbnailKey)
                    .largeS3Key(largeKey)
                    .entityId(java.util.UUID.fromString(entityId))
                    .mediaType(mediaType)
                    .contentType("image/png")
                    .fileSize((long) imageBytes.length)
                    .uploadedBy(event.getStoreId() != null ? java.util.UUID.fromString(event.getStoreId().toString()) : null)
                    .build();

            MediaAsset saved = mediaAssetRepository.save(mediaAsset);

            log.info("✅ MediaAsset created: id={}", saved.getId());

            // Publish MediaUploadedEvent
            MediaUploadedEvent uploadedEvent = new MediaUploadedEvent(
                    saved.getId(),
                    java.util.UUID.fromString(entityId),
                    event.getStoreId() != null ? java.util.UUID.fromString(event.getStoreId().toString()) : null,
                    mediaType,
                    originalUrl,
                    largeUrl,
                    thumbnailUrl
            );

            domainEventPublisher.publish(uploadedEvent);

            log.info("✅ MediaUploadedEvent published: qrType={}, entityId={}", qrType, entityId);

        } catch (Exception e) {
            log.error("❌ Failed to process QR image upload: qrType={}", event.getQrType(), e);
            throw new RuntimeException("Failed to process QR image upload", e);
        }
    }
    
    /**
     * Map QR type string to MediaType enum
     */
    private MediaType mapQRTypeToMediaType(String qrType) {
        return switch (qrType) {
            case "BUSINESS" -> MediaType.BUSINESS_QR;
            case "TABLE" -> MediaType.TABLE_QR;
            case "WIFI" -> MediaType.WIFI_QR;
            default -> throw new IllegalArgumentException("Unknown QR type: " + qrType);
        };
    }
    
    /**
     * Get entity ID based on QR type
     */
    private String getEntityId(QRGeneratedEvent event, String qrType) {
        return switch (qrType) {
            case "BUSINESS" -> event.getBusinessId() != null ? event.getBusinessId().toString() : 
                              (event.getStoreId() != null ? event.getStoreId().toString() : "unknown");
            case "TABLE" -> event.getTableId() != null ? event.getTableId().toString() : 
                           (event.getStoreId() != null ? event.getStoreId().toString() : "unknown");
            case "WIFI" -> event.getWifiSettingsId() != null ? event.getWifiSettingsId().toString() : 
                          (event.getStoreId() != null ? event.getStoreId().toString() : "unknown");
            default -> event.getStoreId() != null ? event.getStoreId().toString() : "unknown";
        };
    }

    /**
     * Resize image using Thumbnailator (modern, simple, no deprecated APIs)
     */
    private byte[] resizeImage(byte[] imageBytes, int width, int height) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Thumbnails.of(new ByteArrayInputStream(imageBytes))
                    .size(width, height)
                    .outputFormat("png")
                    .toOutputStream(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Failed to resize image: {}x{}", width, height, e);
            throw new RuntimeException("Failed to resize image", e);
        }
    }
}
