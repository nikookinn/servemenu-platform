package com.servemenu.userservice.infrastructure.kafka.producer;

import com.servemenu.userservice.domain.event.*;
import com.servemenu.userservice.infrastructure.outbox.OutboxEventService;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.avro.specific.SpecificRecordBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DomainEventPublisher.class);

    private final OutboxEventService outboxEventService;
    private final KafkaAvroSerializer avroSerializer;

    @Value("${kafka.topics.business-owner-created}")
    private String businessOwnerCreatedTopic;

    @Value("${kafka.topics.store-user-created}")
    private String storeUserCreatedTopic;

    @Value("${kafka.topics.customer-created}")
    private String customerCreatedTopic;

    public DomainEventPublisher(
            OutboxEventService outboxEventService, 
            KafkaAvroSerializer avroSerializer) {
        this.outboxEventService = outboxEventService;
        this.avroSerializer = avroSerializer;
        
        log.info("✅ DomainEventPublisher initialized with Avro Serializer (from Config Service)");
    }

    public void publish(Object event) {
        try {
            String eventType = event.getClass().getSimpleName();
            String aggregateType = getAggregateType(event);
            String topic = resolveTopicName(eventType, aggregateType);
            
            // Convert domain event to Avro SpecificRecord
            SpecificRecordBase avroEvent = convertToAvro(event);
            
            // Serialize to Avro binary
            byte[] avroBinary = avroSerializer.serialize(topic, avroEvent);

            // Save to outbox with 2025 standard fields
            outboxEventService.saveEvent(
                    eventType,
                    topic,
                    getAggregateId(event),
                    aggregateType,
                    avroBinary
            );

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
            case BusinessOwnerCreatedEvent e -> com.servemenu.shared.events.avro.BusinessOwnerCreatedEvent.newBuilder()
                    .setEventId(e.eventId().toString())
                    .setEventType("BUSINESS_OWNER_CREATED")
                    .setTimestamp(Instant.ofEpochSecond(e.timestamp().toEpochMilli()))
                    .setUserId(e.userId().toString())
                    .setKeycloakUserId(e.keycloakUserId().toString())
                    .setEmail(e.email())
                    .setFirstName(e.firstName())
                    .setLastName(e.lastName())
                    .build();
            default -> throw new IllegalArgumentException("Unknown event type: " + event.getClass().getSimpleName());
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
            case BusinessOwnerCreatedEvent e -> "user";
            case StoreUserCreatedEvent e -> "user";
            case CustomerCreatedEvent e -> "user";
            default -> throw new IllegalArgumentException("Unknown event type: " + event.getClass().getSimpleName());
        };
    }

    private String getAggregateId(Object event) {
        return switch (event) {
            case BusinessOwnerCreatedEvent e -> e.userId().toString();
            case StoreUserCreatedEvent e -> e.userId().toString();
            case CustomerCreatedEvent e -> e.userId().toString();
            default -> throw new IllegalArgumentException("Unknown event type");
        };
    }
}