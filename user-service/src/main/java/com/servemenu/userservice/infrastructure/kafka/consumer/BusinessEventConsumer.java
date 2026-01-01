package com.servemenu.userservice.infrastructure.kafka.consumer;

import com.servemenu.userservice.application.service.BusinessOwnerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumes business-related events from business-service
 * Keeps user-service in sync with business onboarding status
 */
@Component
public class BusinessEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(BusinessEventConsumer.class);
    private final BusinessOwnerService businessOwnerService;

    public BusinessEventConsumer(BusinessOwnerService businessOwnerService) {
        this.businessOwnerService = businessOwnerService;
    }

    /**
     * Handle BusinessDetailsCompletedEvent from business-service
     * Update BusinessOwnerProfile.onboardingCompleted to keep in sync
     */
    @KafkaListener(
            topics = "${kafka.topics.business-details-completed}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "avroKafkaListenerContainerFactory"
    )
    public void handleBusinessDetailsCompleted(
            @Payload com.servemenu.shared.events.avro.BusinessDetailsCompletedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        String eventId = event.getEventId();
        UUID businessOwnerId = UUID.fromString(event.getBusinessOwnerId());
        UUID businessId = UUID.fromString(event.getBusinessId());
        
        log.info("Processing BUSINESS_DETAILS_COMPLETED event: eventId={}, partition={}, offset={}, ownerId={}",
                eventId, partition, offset, businessOwnerId);

        try {
            // Validate event
            if (event.getBusinessOwnerId() == null || event.getBusinessOwnerId().isEmpty()) {
                log.error("Invalid BUSINESS_DETAILS_COMPLETED event: missing businessOwnerId, eventId={}", eventId);
                acknowledgment.acknowledge();
                return;
            }

            // Update BusinessOwnerProfile onboarding status and businessId
            businessOwnerService.completeOnboarding(businessOwnerId, businessId);

            log.info("BusinessOwner onboarding marked as completed: userId={}, businessId={}, eventId={}",
                    businessOwnerId, businessId, eventId);
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process BUSINESS_DETAILS_COMPLETED event: eventId={}, partition={}, offset={}",
                    eventId, partition, offset, e);
            // Don't acknowledge - let Kafka retry
            throw new RuntimeException("Event processing failed for eventId: " + eventId, e);
        }
    }
}

