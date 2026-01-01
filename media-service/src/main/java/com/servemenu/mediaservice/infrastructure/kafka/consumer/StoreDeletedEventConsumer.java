package com.servemenu.mediaservice.infrastructure.kafka.consumer;

import com.servemenu.mediaservice.application.service.MediaService;
import com.servemenu.shared.events.avro.StoreDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Kafka consumer for STORE_DELETED events
 * Handles BATCH cleanup of QR code media when a store is deleted
 * Professional approach: Single event for all QR codes in the store
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StoreDeletedEventConsumer {

    private final MediaService mediaService;

    @KafkaListener(
            topics = "${kafka.topics.store-deleted}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "avroKafkaListenerContainerFactory"
    )
    public void consumeStoreDeletedEvent(
            @Payload StoreDeletedEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        try {
            log.info("📨 Received StoreDeletedEvent: partition={}, offset={}, key={}, storeId={}, tables={}, qrCodes={}", 
                    partition, offset, key, event.getStoreId(), 
                    event.getTableIds().size(), event.getQrCodeMediaIds().size());

            // Batch delete QR code media
            if (!event.getQrCodeMediaIds().isEmpty()) {
                List<UUID> mediaIds = event.getQrCodeMediaIds().stream()
                        .map(id -> UUID.fromString(id.toString()))
                        .toList();
                
                log.info("🗑️ Batch deleting {} QR code media for store: storeId={}", 
                        mediaIds.size(), event.getStoreId());
                
                // Batch delete from S3 and database
                int deletedCount = mediaService.batchDeleteMedia(mediaIds);
                
                log.info("✅ Batch deleted {} QR code media successfully for store: storeId={}", 
                        deletedCount, event.getStoreId());
            } else {
                log.info("ℹ️ Store has no QR code media to delete: storeId={}", event.getStoreId());
            }

            // Manual acknowledgment after successful processing
            acknowledgment.acknowledge();

            log.info("✅ StoreDeletedEvent processed successfully: storeId={}, qrCodesDeleted={}", 
                    event.getStoreId(), event.getQrCodeMediaIds().size());

        } catch (Exception e) {
            log.error("❌ Failed to process StoreDeletedEvent: partition={}, offset={}, storeId={}, error={}", 
                    partition, offset, event.getStoreId(), e.getMessage(), e);
            
            // Don't acknowledge - message will be reprocessed
            throw new RuntimeException("Failed to process StoreDeletedEvent", e);
        }
    }
}
