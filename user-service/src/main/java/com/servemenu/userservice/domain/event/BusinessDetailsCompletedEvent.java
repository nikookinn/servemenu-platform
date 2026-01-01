package com.servemenu.userservice.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Event received from business-service when business onboarding is completed
 * Used to synchronize onboarding status in user-service
 */
public record BusinessDetailsCompletedEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID businessId,
        UUID businessOwnerId,
        UUID keycloakUserId,
        UUID defaultStoreId,
        boolean isCompleted
) {
    // Record automatically generates constructor, getters, equals, hashCode, toString
}

