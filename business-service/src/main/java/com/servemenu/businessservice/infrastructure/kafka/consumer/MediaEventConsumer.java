package com.servemenu.businessservice.infrastructure.kafka.consumer;

import com.servemenu.businessservice.application.service.BusinessService;
import com.servemenu.businessservice.application.service.TableService;
import com.servemenu.businessservice.application.service.WiFiSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumes media-related events from media-service using Avro deserialization
 * Updates entities with QR code media IDs after QR generation and upload
 * Handles: BUSINESS_QR, TABLE_QR, WIFI_QR
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MediaEventConsumer {

    private final TableService tableService;
    private final BusinessService businessService;
    private final WiFiSettingsService wifiSettingsService;

    /**
     * Handle MediaUploadedEvent from media-service (Avro format)
     * Routes to appropriate service based on mediaType:
     * - BUSINESS_QR → Update Business.businessQrMediaId
     * - TABLE_QR → Update Table.qrCodeMediaId
     * - WIFI_QR → Update WifiSettings.qrCodeMediaId
     * 
     * Flow:
     * 1. Business/Table/WiFi created → QR generation requested
     * 2. QR Service generates QR image → publishes QRGeneratedEvent
     * 3. Media Service uploads to S3 → publishes MediaUploadedEvent (Avro)
     * 4. Business Service updates entity with media ID (THIS METHOD)
     */
    @KafkaListener(
            topics = "${kafka.topics.media-uploaded}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "avroKafkaListenerContainerFactory"
    )
    public void handleMediaUploaded(
            @Payload com.servemenu.shared.events.avro.MediaUploadedEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        try {
            log.info("📨 Received MediaUploadedEvent (Avro): partition={}, offset={}, key={}", partition, offset, key);

            String mediaType = event.getMediaType();
            UUID entityId = UUID.fromString(event.getEntityId());
            UUID mediaId = UUID.fromString(event.getMediaId());
            
            log.info("🎨 Processing media upload: mediaId={}, entityId={}, mediaType={}", 
                    mediaId, entityId, mediaType);

            // Route to appropriate service based on media type
            switch (mediaType) {
                case "BUSINESS_QR" -> {
                    businessService.updateBusinessQRMediaId(entityId, mediaId);
                    log.info("✅ Business updated with QR media ID: businessId={}, mediaId={}", entityId, mediaId);
                }
                case "TABLE_QR" -> {
                    tableService.updateTableWithQRMediaId(entityId, mediaId);
                    log.info("✅ Table updated with QR media ID: tableId={}, mediaId={}", entityId, mediaId);
                }
                case "WIFI_QR" -> {
                    wifiSettingsService.updateWiFiSettingsWithQRMediaId(entityId, mediaId);
                    log.info("✅ WiFi settings updated with QR media ID: wifiSettingsId={}, mediaId={}", entityId, mediaId);
                }
                default -> {
                    log.warn("⚠️ Unknown media type, skipping: {}", mediaType);
                }
            }
            
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("❌ Failed to process MediaUploadedEvent: partition={}, offset={}", partition, offset, e);
            // Don't acknowledge - message will be reprocessed
            throw new RuntimeException("Failed to process MediaUploadedEvent", e);
        }
    }
}
