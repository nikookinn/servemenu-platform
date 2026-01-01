package com.servemenu.mediaservice.infrastructure.kafka.consumer;

import com.servemenu.mediaservice.application.service.QRMediaUploadService;
import com.servemenu.shared.events.avro.QRGeneratedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class QRGeneratedEventConsumer {

    private final QRMediaUploadService qrMediaUploadService;

    @KafkaListener(
            topics = "${kafka.topics.qr-generated}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "avroKafkaListenerContainerFactory"
    )
    public void consumeQRGeneratedEvent(
            @Payload QRGeneratedEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        try {
            log.info("📨 Received QRGeneratedEvent: partition={}, offset={}, key={}, tableId={}", 
                    partition, offset, key, event.getTableId());

            log.info("🎨 Processing QR upload for table: {}", event.getTableId());

            // Upload QR image to S3 and create media asset
            qrMediaUploadService.processQRGenerated(event);

            // Manual acknowledgment after successful processing
            acknowledgment.acknowledge();

            log.info("✅ QRGeneratedEvent processed successfully: tableId={}", event.getTableId());

        } catch (Exception e) {
            log.error("❌ Failed to process QRGeneratedEvent: partition={}, offset={}", partition, offset, e);
            // Don't acknowledge - message will be reprocessed
            throw new RuntimeException("Failed to process QRGeneratedEvent", e);
        }
    }
}
