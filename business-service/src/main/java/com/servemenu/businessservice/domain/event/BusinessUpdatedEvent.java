package com.servemenu.businessservice.domain.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BusinessUpdatedEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID businessId,
        UUID businessOwnerId,
        String businessName,
        List<String> changedFields
) {
    public BusinessUpdatedEvent(
            UUID businessId,
            UUID businessOwnerId,
            String businessName,
            List<String> changedFields
    ) {
        this(
                UUID.randomUUID(),
                "BUSINESS_UPDATED",
                Instant.now(),
                businessId,
                businessOwnerId,
                businessName,
                changedFields
        );
    }
}
