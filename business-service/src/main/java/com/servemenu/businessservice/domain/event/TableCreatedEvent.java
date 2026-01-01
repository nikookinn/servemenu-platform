package com.servemenu.businessservice.domain.event;

import java.time.Instant;
import java.util.UUID;

public record TableCreatedEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID tableId,
        UUID storeId,
        UUID businessId,
        String tableName,
        String tableNumber,
        String qrUrl
) {
    public TableCreatedEvent(
            UUID tableId,
            UUID storeId,
            UUID businessId,
            String tableName,
            String tableNumber,
            String qrUrl
    ) {
        this(
                UUID.randomUUID(),
                "TABLE_CREATED",
                Instant.now(),
                tableId,
                storeId,
                businessId,
                tableName,
                tableNumber,
                qrUrl
        );
    }
}
