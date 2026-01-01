package com.servemenu.businessservice.infrastructure.kafka.consumer;

import com.servemenu.businessservice.domain.enums.BusinessStatus;
import com.servemenu.businessservice.domain.model.Business;
import com.servemenu.businessservice.domain.repository.BusinessRepository;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final BusinessRepository businessRepository;

    /**
     * Listen to BusinessOwnerCreatedEvent from user-service (Avro via Debezium)
     * Create empty Business profile (to be filled during onboarding)
     */
    @KafkaListener(
            topics = "${kafka.topics.business-owner-created}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "avroKafkaListenerContainerFactory"
    )
    public void handleBusinessOwnerCreated(
            @Payload com.servemenu.shared.events.avro.BusinessOwnerCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        String eventId = event.getEventId();
        UUID userId = UUID.fromString(event.getUserId());
        UUID keycloakUserId = UUID.fromString(event.getKeycloakUserId());
        
        log.info("Processing BUSINESS_OWNER_CREATED event (Avro): eventId={}, partition={}, offset={}, userId={}",
                eventId, partition, offset, userId);

        try {

            // Idempotency check
            if (businessRepository.existsByBusinessOwnerId(userId)) {
                log.warn("Business profile already exists (idempotency): userId={}, eventId={}",
                        userId, eventId);
                acknowledgment.acknowledge();
                return;
            }

            // Create empty business profile (will be filled during onboarding)
            Business business = Business.builder()
                    .businessOwnerId(userId)
                    .keycloakUserId(keycloakUserId)
                    .status(BusinessStatus.DRAFT)
                    .isOnboardingCompleted(false)
                    .build();

            businessRepository.save(business);

            log.info("Empty business profile created: businessId={}, ownerId={}",
                    business.getId(), userId);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process BUSINESS_OWNER_CREATED event: eventId={}, partition={}, offset={}",
                    eventId, partition, offset, e);
            // Don't acknowledge - let Kafka retry
            throw new RuntimeException("Event processing failed for eventId: " + eventId, e);
        }
    }
}