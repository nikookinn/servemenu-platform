package com.servemenu.businessservice.domain.event;

import java.time.Instant;
import java.util.UUID;

public record TableDeletedEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID tableId,
        UUID storeId,
        UUID qrCodeMediaId
) {
    public TableDeletedEvent(UUID tableId, UUID storeId, UUID qrCodeMediaId) {
        this(
                UUID.randomUUID(),
                "TABLE_DELETED",
                Instant.now(),
                tableId,
                storeId,
                qrCodeMediaId
        );
    }
}
