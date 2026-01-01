package com.servemenu.mediaservice.infrastructure.kafka.producer;

import com.servemenu.mediaservice.domain.event.MediaUploadedEvent;
import com.servemenu.mediaservice.domain.event.QRGeneratedEvent;
import com.servemenu.mediaservice.domain.model.OutboxEvent;
import com.servemenu.mediaservice.domain.repository.OutboxEventRepository;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain Event Publisher with Avro Serialization
 * Converts domain events to Avro SpecificRecords and serializes to binary
 * Debezium reads binary Avro and publishes to Kafka
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DomainEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaAvroSerializer avroSerializer;
    private final Tracer tracer;
    
    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * Publish domain event as Avro binary to outbox table
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(Object event) {
        try {
            String eventType = event.getClass().getSimpleName();
            String aggregateType = getAggregateType(event);
            String aggregateId = getAggregateId(event);
            String topic = resolveTopicName(eventType, aggregateType);
            
            // Get distributed tracing context
            String traceId = Optional.ofNullable(tracer.currentSpan())
                    .map(span -> span.context().traceId())
                    .orElse(UUID.randomUUID().toString());
            
            String correlationId = Optional.ofNullable(tracer.currentSpan())
                    .flatMap(span -> Optional.ofNullable(span.context().spanId()))
                    .orElse(UUID.randomUUID().toString());
            
            // Convert domain event to Avro SpecificRecord
            SpecificRecordBase avroEvent = convertToAvro(event);
            
            // Serialize to Avro binary
            byte[] avroBinary = avroSerializer.serialize(topic, avroEvent);
            
            // Save to outbox with 2025 standard fields
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventType(eventType)
                    .topic(topic)
                    .aggregateId(aggregateId)
                    .aggregateType(aggregateType)
                    .payload(avroBinary)
                    .traceId(traceId)
                    .correlationId(correlationId)
                    .schemaVersion(1)
                    .source(applicationName)
                    .build();
            
            outboxEventRepository.save(outboxEvent);
            
            log.debug("Domain event saved to outbox (Avro): type={}, topic={}, size={} bytes", 
                    eventType, topic, avroBinary.length);
            
        } catch (Exception e) {
            log.error("Failed to serialize domain event: {}", event.getClass().getSimpleName(), e);
            throw new RuntimeException("Failed to serialize domain event", e);
        }
    }
    
    /**
     * Convert domain event to Avro SpecificRecord
     */
    private SpecificRecordBase convertToAvro(Object event) {
        return switch (event) {
            case MediaUploadedEvent e -> com.servemenu.shared.events.avro.MediaUploadedEvent.newBuilder()
                    .setEventId(e.eventId().toString())
                    .setEventType("MEDIA_UPLOADED")
                    .setTimestamp(e.timestamp().toEpochMilli())
                    .setMediaId(e.mediaId().toString())
                    .setEntityId(e.entityId().toString())
                    .setStoreId(e.storeId() != null ? e.storeId().toString() : null)
                    .setMediaType(e.mediaType().name())
                    .setOriginalUrl(e.originalUrl())
                    .setLargeUrl(e.largeUrl())
                    .setThumbnailUrl(e.thumbnailUrl())
                    .build();
            case QRGeneratedEvent e -> com.servemenu.shared.events.avro.QRGeneratedEvent.newBuilder()
                    .setEventId(e.eventId().toString())
                    .setEventType("QR_GENERATED")
                    .setTimestamp(Instant.ofEpochMilli(e.timestamp().toEpochMilli()))
                    .setTableId(e.tableId().toString())
                    .setStoreId(e.storeId().toString())
                    .setQrImageBase64(e.qrImageBase64())
                    .setQrImageSize(e.qrImageBase64().length())
                    .setQrUrl(e.qrUrl())
                    .build();
            default -> throw new IllegalArgumentException("Unknown event type for Avro conversion: " + event.getClass().getSimpleName());
        };
    }
    
    /**
     * Resolve topic name using 2025 pattern: events.{aggregate_type}.{event_type}
     */
    private String resolveTopicName(String eventType, String aggregateType) {
        // Remove "Event" suffix from event type
        String eventName = eventType.replace("Event", "");
        
        // Format: events.{aggregate_type}.{EventType}
        return String.format("events.%s.%s", aggregateType, eventName);
    }
    
    /**
     * Get aggregate type from event
     */
    private String getAggregateType(Object event) {
        return switch (event) {
            case MediaUploadedEvent e -> "media";
            case QRGeneratedEvent e -> "qr";
            default -> throw new IllegalArgumentException(
                    "Unknown event type: " + event.getClass().getSimpleName());
        };
    }
    
    /**
     * Get aggregate ID from event
     */
    private String getAggregateId(Object event) {
        return switch (event) {
            case MediaUploadedEvent e -> e.mediaId().toString();
            case QRGeneratedEvent e -> e.tableId().toString();
            default -> throw new IllegalArgumentException(
                    "Unknown event type: " + event.getClass().getSimpleName());
        };
    }
}
