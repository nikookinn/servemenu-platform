package com.servemenu.mediaservice.infrastructure.kafka.consumer;

import com.servemenu.mediaservice.application.service.MediaService;
import com.servemenu.shared.events.avro.WifiSettingsDeletedEvent;
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
 * Kafka consumer for WIFI_SETTINGS_DELETED events
 * Handles cleanup of QR code media when WiFi settings are deleted (soft delete)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WifiSettingsDeletedEventConsumer {

    private final MediaService mediaService;

    @KafkaListener(
            topics = "${kafka.topics.wifi-settings-deleted}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "avroKafkaListenerContainerFactory"
    )
    public void consumeWifiSettingsDeletedEvent(
            @Payload WifiSettingsDeletedEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        try {
            log.info("📨 Received WifiSettingsDeletedEvent: partition={}, offset={}, key={}, wifiSettingsId={}, qrCodeMediaId={}", 
                    partition, offset, key, event.getWifiSettingsId(), event.getQrCodeMediaId());

            // If WiFi settings has QR code media, delete it
            if (event.getQrCodeMediaId() != null) {
                UUID mediaId = UUID.fromString(event.getQrCodeMediaId().toString());
                
                log.info("🗑️ Deleting WiFi QR code media: mediaId={}, wifiSettingsId={}", mediaId, event.getWifiSettingsId());
                
                // Delete media (removes from S3 and database)
                mediaService.deleteMedia(mediaId);
                
                log.info("✅ WiFi QR code media deleted successfully: mediaId={}, wifiSettingsId={}", mediaId, event.getWifiSettingsId());
            } else {
                log.info("ℹ️ WiFi settings has no QR code media to delete: wifiSettingsId={}", event.getWifiSettingsId());
            }

            // Manual acknowledgment after successful processing
            acknowledgment.acknowledge();

            log.info("✅ WifiSettingsDeletedEvent processed successfully: wifiSettingsId={}", event.getWifiSettingsId());

        } catch (Exception e) {
            log.error("❌ Failed to process WifiSettingsDeletedEvent: partition={}, offset={}, wifiSettingsId={}, error={}", 
                    partition, offset, event.getWifiSettingsId(), e.getMessage(), e);
            
            // Don't acknowledge - message will be reprocessed
            throw new RuntimeException("Failed to process WifiSettingsDeletedEvent", e);
        }
    }
}
