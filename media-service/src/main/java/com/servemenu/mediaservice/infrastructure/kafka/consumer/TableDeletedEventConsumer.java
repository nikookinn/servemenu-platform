package com.servemenu.mediaservice.infrastructure.kafka.consumer;

import com.servemenu.mediaservice.application.service.MediaService;
import com.servemenu.shared.events.avro.TableDeletedEvent;
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
 * Kafka consumer for TABLE_DELETED events
 * Handles cleanup of QR code media when a table is deleted (soft delete)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TableDeletedEventConsumer {

    private final MediaService mediaService;

    @KafkaListener(
            topics = "${kafka.topics.table-deleted}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "avroKafkaListenerContainerFactory"
    )
    public void consumeTableDeletedEvent(
            @Payload TableDeletedEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        try {
            log.info("📨 Received TableDeletedEvent: partition={}, offset={}, key={}, tableId={}, qrCodeMediaId={}", 
                    partition, offset, key, event.getTableId(), event.getQrCodeMediaId());

            // If table has QR code media, delete it
            if (event.getQrCodeMediaId() != null) {
                UUID mediaId = UUID.fromString(event.getQrCodeMediaId().toString());
                
                log.info("🗑️ Deleting QR code media: mediaId={}, tableId={}", mediaId, event.getTableId());
                
                // Delete media (removes from S3 and database)
                mediaService.deleteMedia(mediaId);
                
                log.info("✅ QR code media deleted successfully: mediaId={}, tableId={}", mediaId, event.getTableId());
            } else {
                log.info("ℹ️ Table has no QR code media to delete: tableId={}", event.getTableId());
            }

            // Manual acknowledgment after successful processing
            acknowledgment.acknowledge();

            log.info("✅ TableDeletedEvent processed successfully: tableId={}", event.getTableId());

        } catch (Exception e) {
            log.error("❌ Failed to process TableDeletedEvent: partition={}, offset={}, tableId={}, error={}", 
                    partition, offset, event.getTableId(), e.getMessage(), e);
            
            // Don't acknowledge - message will be reprocessed
            throw new RuntimeException("Failed to process TableDeletedEvent", e);
        }
    }
}
