package com.servemenu.businessservice.domain.event;

import java.time.Instant;
import java.util.UUID;

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
    public BusinessDetailsCompletedEvent(
            UUID businessId,
            UUID businessOwnerId,
            UUID keycloakUserId,
            UUID defaultStoreId
    ) {
        this(
                UUID.randomUUID(),
                "BUSINESS_DETAILS_COMPLETED",
                Instant.now(),
                businessId,
                businessOwnerId,
                keycloakUserId,
                defaultStoreId,
                true
        );
    }
}
