package com.servemenu.businessservice.infrastructure.kafka.producer;

import com.servemenu.businessservice.domain.event.*;
import com.servemenu.businessservice.infrastructure.outbox.OutboxEventService;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.avro.specific.SpecificRecordBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Domain Event Publisher with Avro Serialization
 * Converts domain events to Avro SpecificRecords and serializes to binary
 * Debezium reads binary Avro and publishes to Kafka
 */
@Component
public class DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DomainEventPublisher.class);

    private final OutboxEventService outboxEventService;
    private final KafkaAvroSerializer avroSerializer;

    public DomainEventPublisher(
            OutboxEventService outboxEventService,
            KafkaAvroSerializer avroSerializer) {
        this.outboxEventService = outboxEventService;
        this.avroSerializer = avroSerializer;
        
        log.info("✅ DomainEventPublisher initialized with Avro Serializer");
    }

    /**
     * Publish domain event as Avro binary to outbox table
     */
    public void publish(Object event) {
        try {
            String eventType = event.getClass().getSimpleName();
            String aggregateType = getAggregateType(event);
            String topic = resolveTopicName(eventType, aggregateType);
            String aggregateId = getAggregateId(event);

            // Convert domain event to Avro SpecificRecord
            SpecificRecordBase avroEvent = convertToAvro(event);
            
            // Serialize to Avro binary
            byte[] avroBinary = avroSerializer.serialize(topic, avroEvent);

            // Save to outbox with standard fields
            outboxEventService.saveEvent(eventType, topic, aggregateId, aggregateType, avroBinary);

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
            case BusinessCreatedEvent e -> com.servemenu.shared.events.avro.BusinessCreatedEvent.newBuilder()
                    .setEventId(e.eventId().toString())
                    .setEventType("BUSINESS_CREATED")
                    .setTimestamp(Instant.ofEpochMilli(e.timestamp().toEpochMilli()))
                    .setBusinessId(e.businessId().toString())
                    .setBusinessOwnerId(e.businessOwnerId().toString())
                    .setKeycloakUserId(e.keycloakUserId().toString())
                    .setBusinessName(e.businessName())
                    .setSlug(e.slug())
                    .setCurrency(e.currency())
                    .setSupportedLanguages(e.supportedLanguages())
                    .build();
            case StoreCreatedEvent e -> com.servemenu.shared.events.avro.StoreCreatedEvent.newBuilder()
                    .setEventId(e.eventId().toString())
                    .setEventType("STORE_CREATED")
                    .setTimestamp(Instant.ofEpochMilli(e.timestamp().toEpochMilli()))
                    .setStoreId(e.storeId().toString())
                    .setBusinessId(e.businessId().toString())
                    .setStoreName(e.storeName())
                    .setIsDefault(e.isDefault())
                    .build();
            case TableCreatedEvent e -> com.servemenu.shared.events.avro.TableCreatedEvent.newBuilder()
                    .setEventId(e.eventId().toString())
                    .setEventType("TABLE_CREATED")
                    .setTimestamp(Instant.ofEpochMilli(e.timestamp().toEpochMilli()))
                    .setTableId(e.tableId().toString())
                    .setStoreId(e.storeId().toString())
                    .setBusinessId(e.businessId().toString())
                    .setTableName(e.tableName())
                    .setTableNumber(e.tableNumber())
                    .setQrUrl(e.qrUrl())
                    .build();
            case BusinessDetailsCompletedEvent e -> com.servemenu.shared.events.avro.BusinessDetailsCompletedEvent.newBuilder()
                    .setEventId(e.eventId().toString())
                    .setEventType("BUSINESS_DETAILS_COMPLETED")
                    .setTimestamp(Instant.ofEpochMilli(e.timestamp().toEpochMilli()))
                    .setBusinessId(e.businessId().toString())
                    .setBusinessOwnerId(e.businessOwnerId().toString())
                    .setKeycloakUserId(e.keycloakUserId().toString())
                    .setDefaultStoreId(e.defaultStoreId().toString())
                    .setIsCompleted(e.isCompleted())
                    .build();
            case QRGenerationRequestedEvent e -> com.servemenu.shared.events.avro.QRGenerationRequestedEvent.newBuilder()
                    .setEventId(e.eventId().toString())
                    .setEventType("QR_GENERATION_REQUESTED")
                    .setTimestamp(Instant.ofEpochMilli(e.timestamp().toEpochMilli()))
                    .setStoreId(e.storeId() != null ? e.storeId().toString() : null) // Business QR has no storeId
                    .setBusinessId(e.businessId() != null ? e.businessId().toString() : null)
                    .setQrType(e.qrType().name())
                    .setTargetUrl(e.targetUrl())
                    .setMetadata(convertMetadataToStringMap(e.metadata()))
                    .build();
            case TableDeletedEvent e -> com.servemenu.shared.events.avro.TableDeletedEvent.newBuilder()
                    .setEventId(e.eventId().toString())
                    .setEventType("TABLE_DELETED")
                    .setTimestamp(Instant.ofEpochMilli(e.timestamp().toEpochMilli()))
                    .setTableId(e.tableId().toString())
                    .setStoreId(e.storeId().toString())
                    .setQrCodeMediaId(e.qrCodeMediaId() != null ? e.qrCodeMediaId().toString() : null)
                    .build();
            case com.servemenu.businessservice.domain.event.StoreDeletedEvent e -> com.servemenu.shared.events.avro.StoreDeletedEvent.newBuilder()
                    .setEventId(e.eventId().toString())
                    .setEventType("STORE_DELETED")
                    .setTimestamp(Instant.ofEpochMilli(e.timestamp().toEpochMilli()))
                    .setStoreId(e.storeId().toString())
                    .setBusinessId(e.businessId().toString())
                    .setTableIds(e.tableIds().stream().map(UUID::toString).toList())
                    .setQrCodeMediaIds(e.qrCodeMediaIds().stream().map(UUID::toString).toList())
                    .build();
            case WifiSettingsDeletedEvent e -> com.servemenu.shared.events.avro.WifiSettingsDeletedEvent.newBuilder()
                    .setEventId(e.eventId().toString())
                    .setEventType("WIFI_SETTINGS_DELETED")
                    .setTimestamp(Instant.ofEpochMilli(e.timestamp().toEpochMilli()))
                    .setWifiSettingsId(e.wifiSettingsId().toString())
                    .setStoreId(e.storeId().toString())
                    .setQrCodeMediaId(e.qrCodeMediaId() != null ? e.qrCodeMediaId().toString() : null)
                    .build();
            default -> throw new IllegalArgumentException("Unknown event type for Avro conversion: " + event.getClass().getSimpleName());
        };
    }

    // Convenience methods for type safety
    public void publishTableCreatedEvent(TableCreatedEvent event) {
        publish(event);
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
     * All events from Business Service use "business" as aggregate type
     */
    private String getAggregateType(Object event) {
        return switch (event) {
            case BusinessCreatedEvent e -> "business";
            case BusinessDetailsCompletedEvent e -> "business";
            case BusinessUpdatedEvent e -> "business";
            case BusinessDeletedEvent e -> "business";
            case StoreCreatedEvent e -> "business";  // Business Service owns Store
            case StoreUpdatedEvent e -> "business";
            case StoreDeletedEvent e -> "business";
            case TableCreatedEvent e -> "business";  // Business Service owns Table
            case TableUpdatedEvent e -> "business";
            case TableDeletedEvent e -> "business";
            case WifiSettingsDeletedEvent e -> "business";  // Business Service owns WifiSettings
            case MenuAssignedEvent e -> "business";
            case QRGenerationRequestedEvent e -> "business";  // Business Service requests QR generation
            default -> throw new IllegalArgumentException("Unknown event type: " + event.getClass().getSimpleName());
        };
    }

    private String getAggregateId(Object event) {
        return switch (event) {
            case BusinessCreatedEvent e -> e.businessId().toString();
            case BusinessDetailsCompletedEvent e -> e.businessId().toString();
            case BusinessUpdatedEvent e -> e.businessId().toString();
            case BusinessDeletedEvent e -> e.businessId().toString();
            case StoreCreatedEvent e -> e.storeId().toString();
            case StoreUpdatedEvent e -> e.storeId().toString();
            case StoreDeletedEvent e -> e.storeId().toString();
            case TableCreatedEvent e -> e.tableId().toString();
            case TableUpdatedEvent e -> e.tableId().toString();
            case TableDeletedEvent e -> e.tableId().toString();
            case MenuAssignedEvent e -> e.storeId().toString();
            case QRGenerationRequestedEvent e -> e.storeId() != null 
                ? e.storeId().toString() 
                : e.businessId().toString(); // Business QR has no storeId
            default -> throw new IllegalArgumentException("Unknown event type");
        };
    }
    
    /**
     * Convert metadata Map<String, Object> to Map<String, String> for Avro
     */
    private Map<String, String> convertMetadataToStringMap(Map<String, Object> metadata) {
        if (metadata == null) {
            return new HashMap<>();
        }
        return metadata.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue() != null ? entry.getValue().toString() : ""
                ));
    }
}
