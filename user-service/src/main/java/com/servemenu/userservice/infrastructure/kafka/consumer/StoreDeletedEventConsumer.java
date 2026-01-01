package com.servemenu.userservice.infrastructure.kafka.consumer;

import com.servemenu.shared.events.avro.StoreDeletedEvent;
import com.servemenu.userservice.application.service.StoreUserService;
import com.servemenu.userservice.domain.model.StoreUserProfile;
import com.servemenu.userservice.domain.repository.StoreUserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Kafka consumer for STORE_DELETED events
 * Handles batch deactivation of store users when a store is deleted
 * Professional approach: Batch process all users in the store
 */
@Component
public class StoreDeletedEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(StoreDeletedEventConsumer.class);

    private final StoreUserService storeUserService;
    private final StoreUserProfileRepository storeUserProfileRepository;

    public StoreDeletedEventConsumer(
            StoreUserService storeUserService,
            StoreUserProfileRepository storeUserProfileRepository
    ) {
        this.storeUserService = storeUserService;
        this.storeUserProfileRepository = storeUserProfileRepository;
    }

    @KafkaListener(
            topics = "${kafka.topics.store-deleted}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "avroKafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeStoreDeletedEvent(
            @Payload StoreDeletedEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        try {
            UUID storeId = UUID.fromString(event.getStoreId());
            
            log.info("📨 Received StoreDeletedEvent: partition={}, offset={}, key={}, storeId={}, businessId={}", 
                    partition, offset, key, storeId, event.getBusinessId());

            // Find all active store users for this store
            List<StoreUserProfile> storeUsers = storeUserProfileRepository
                    .findByStoreIdAndIsActive(storeId, true);
            
            if (!storeUsers.isEmpty()) {
                log.info("🗑️ Batch deactivating {} store users for deleted store: storeId={}", 
                        storeUsers.size(), storeId);
                
                int deactivatedCount = 0;
                int failedCount = 0;
                
                // Deactivate each store user
                for (StoreUserProfile profile : storeUsers) {
                    try {
                        // Use existing deactivation logic (handles Keycloak + DB)
                        storeUserService.deactivateStoreUser(profile.getUser().getId());
                        deactivatedCount++;
                        
                        log.debug("Deactivated store user: userId={}, storeId={}", 
                                profile.getUser().getId(), storeId);
                                
                    } catch (Exception e) {
                        failedCount++;
                        log.error("Failed to deactivate store user: userId={}, storeId={}, error={}", 
                                profile.getUser().getId(), storeId, e.getMessage());
                        // Continue with other users
                    }
                }
                
                log.info("✅ Batch deactivation completed for store: storeId={}, deactivated={}, failed={}", 
                        storeId, deactivatedCount, failedCount);
                
                // If any users failed, we might want to retry or alert
                if (failedCount > 0) {
                    log.warn("⚠️ {} store users failed to deactivate for store: storeId={}", 
                            failedCount, storeId);
                }
            } else {
                log.info("ℹ️ No active store users to deactivate for store: storeId={}", storeId);
            }

            // Manual acknowledgment after successful processing
            acknowledgment.acknowledge();

            log.info("✅ StoreDeletedEvent processed successfully: storeId={}, usersDeactivated={}", 
                    storeId, storeUsers.size());

        } catch (Exception e) {
            log.error("❌ Failed to process StoreDeletedEvent: partition={}, offset={}, storeId={}, error={}", 
                    partition, offset, event.getStoreId(), e.getMessage(), e);
            
            // Don't acknowledge - message will be reprocessed
            throw new RuntimeException("Failed to process StoreDeletedEvent", e);
        }
    }
}
