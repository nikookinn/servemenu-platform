package com.servemenu.businessservice.domain.event;

import java.time.Instant;
import java.util.UUID;

public record MenuAssignedEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID storeId,
        UUID businessId,
        UUID menuId
) {
    public MenuAssignedEvent(UUID storeId, UUID businessId, UUID menuId) {
        this(
                UUID.randomUUID(),
                "MENU_ASSIGNED",
                Instant.now(),
                storeId,
                businessId,
                menuId
        );
    }
}
