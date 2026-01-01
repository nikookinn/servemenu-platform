package com.servemenu.businessservice.domain.event;

import java.time.Instant;
import java.util.UUID;

public record StoreCreatedEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID storeId,
        UUID businessId,
        String storeName,
        boolean isDefault
) {
    public StoreCreatedEvent(
            UUID storeId,
            UUID businessId,
            String storeName,
            boolean isDefault
    ) {
        this(
                UUID.randomUUID(),
                "STORE_CREATED",
                Instant.now(),
                storeId,
                businessId,
                storeName,
                isDefault
        );
    }
}
