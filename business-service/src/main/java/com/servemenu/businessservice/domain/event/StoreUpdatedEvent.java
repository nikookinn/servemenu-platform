package com.servemenu.businessservice.domain.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StoreUpdatedEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID storeId,
        UUID businessId,
        String storeName,
        List<String> changedFields
) {
    public StoreUpdatedEvent(
            UUID storeId,
            UUID businessId,
            String storeName,
            List<String> changedFields
    ) {
        this(
                UUID.randomUUID(),
                "STORE_UPDATED",
                Instant.now(),
                storeId,
                businessId,
                storeName,
                changedFields
        );
    }
}
