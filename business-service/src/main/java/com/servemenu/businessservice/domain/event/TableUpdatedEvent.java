package com.servemenu.businessservice.domain.event;

import java.time.Instant;
import java.util.UUID;

public record TableUpdatedEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID tableId,
        UUID storeId,
        String tableName
) {
    public TableUpdatedEvent(UUID tableId, UUID storeId, String tableName) {
        this(
                UUID.randomUUID(),
                "TABLE_UPDATED",
                Instant.now(),
                tableId,
                storeId,
                tableName
        );
    }
}
