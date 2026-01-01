package com.servemenu.userservice.infrastructure.kafka.consumer;

import com.servemenu.userservice.application.service.BusinessOwnerService;
import com.servemenu.userservice.application.service.CustomerService;
import com.servemenu.userservice.application.service.StoreUserService;
import com.servemenu.userservice.application.service.UserService;
import com.servemenu.userservice.common.exception.DuplicateResourceException;
import com.servemenu.userservice.domain.event.CustomerRegisteredEvent;
import com.servemenu.userservice.domain.event.EmailVerifiedEvent;
import com.servemenu.userservice.domain.event.StoreUserRegisteredEvent;
import com.servemenu.userservice.domain.event.UserRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class KeycloakEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(KeycloakEventConsumer.class);
    private final BusinessOwnerService businessOwnerService;
    private final UserService userService;

    public KeycloakEventConsumer(
            BusinessOwnerService businessOwnerService,
            UserService userService
    ) {
        this.businessOwnerService = businessOwnerService;
        this.userService = userService;
    }

    @KafkaListener(
            topics = "${kafka.topics.user-registered}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "userRegisteredListenerContainerFactory"
    )
    public void handleUserRegistered(
            @Payload UserRegisteredEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        String eventId = event.eventId();
        log.info("Processing USER_REGISTERED event: eventId={}, partition={}, offset={}, userId={}",
                eventId, partition, offset, event.user().id());

        try {
            // Validate event
            if (event.user().id() == null) {
                log.error("Invalid USER_REGISTERED event: missing user data, eventId={}", eventId);
                acknowledgment.acknowledge();
                return;
            }

            UUID keycloakUserId = parseUUID(event.user().id(), eventId);
            if (keycloakUserId == null) {
                acknowledgment.acknowledge();
                return;
            }

            // Idempotency check - by keycloakUserId
            if (businessOwnerService.existsByKeycloakUserId(keycloakUserId)) {
                log.warn("Business owner already exists (idempotency): keycloakUserId={}, eventId={}",
                        keycloakUserId, eventId);
                acknowledgment.acknowledge();
                return;
            }
            
            // Idempotency check - by email (race condition protection)
            if (userService.existsByEmail(event.user().email())) {
                log.warn("User with email already exists (idempotency): email={}, eventId={}",
                        event.user().email(), eventId);
                acknowledgment.acknowledge();
                return;
            }

            // Create business owner
            businessOwnerService.createBusinessOwner(
                    keycloakUserId,
                    event.user().email(),
                    event.user().firstName(),
                    event.user().lastName(),
                    event.user().emailVerified()
            );

            log.info("Business owner created successfully: keycloakUserId={}, eventId={}",
                    keycloakUserId, eventId);
            acknowledgment.acknowledge();

        } catch (DuplicateResourceException e) {
            // Handle race condition - acknowledge to avoid infinite retry
            log.warn("Duplicate business owner detected (race condition): eventId={}", eventId, e);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process USER_REGISTERED event: eventId={}, partition={}, offset={}",
                    eventId, partition, offset, e);
            // Don't acknowledge - let Kafka retry
            throw new RuntimeException("Event processing failed for eventId: " + eventId, e);
        }
    }


    /**
     * Handle email verification events from Keycloak
     */
    @KafkaListener(
            topics = "${kafka.topics.email-verified}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "emailVerifiedListenerContainerFactory"
    )
    public void handleEmailVerified(
            @Payload EmailVerifiedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        String eventId = event.eventId();
        log.info("Processing EMAIL_VERIFIED event: eventId={}, partition={}, offset={}, userId={}",
                eventId, partition, offset, event.userId());

        try {
            // Validate event
            if (event.userId() == null || event.userId().trim().isEmpty()) {
                log.error("Invalid EMAIL_VERIFIED event: missing userId, eventId={}", eventId);
                acknowledgment.acknowledge();
                return;
            }

            UUID keycloakUserId = parseUUID(event.userId(), eventId);
            if (keycloakUserId == null) {
                acknowledgment.acknowledge();
                return;
            }

            // Check if user exists before updating
            if (!userService.existsByKeycloakUserId(keycloakUserId)) {
                log.warn("User not found for email verification: keycloakUserId={}, eventId={}",
                        keycloakUserId, eventId);
                acknowledgment.acknowledge();
                return;
            }

            // Verify email
            userService.verifyEmailByKeycloakUserId(keycloakUserId);

            log.info("Email verified successfully: keycloakUserId={}, eventId={}",
                    keycloakUserId, eventId);
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process EMAIL_VERIFIED event: eventId={}, partition={}, offset={}",
                    eventId, partition, offset, e);
            // Don't acknowledge - let Kafka retry
            throw new RuntimeException("Email verification event processing failed for eventId: " + eventId, e);
        }
    }

    private UUID parseUUID(String uuidString, String eventId) {
        try {
            return UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format in event: uuid={}, eventId={}", uuidString, eventId);
            return null;
        }
    }

}