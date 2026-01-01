package com.servemenu.businessservice.domain.event;

import java.time.Instant;
import java.util.UUID;

public record BusinessDeletedEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID businessId,
        UUID businessOwnerId
) {
    public BusinessDeletedEvent(UUID businessId, UUID businessOwnerId) {
        this(
                UUID.randomUUID(),
                "BUSINESS_DELETED",
                Instant.now(),
                businessId,
                businessOwnerId
        );
    }
}
