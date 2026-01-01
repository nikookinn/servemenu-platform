package com.servemenu.businessservice.domain.event;

import java.time.Instant;
import java.util.UUID;

public record TableQRRegenerationEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID tableId,
        UUID storeId,
        UUID businessId,
        String tableName,
        String tableNumber,
        UUID qrCustomizationId,
        UUID performedBy
) {
    public TableQRRegenerationEvent(
            UUID tableId,
            UUID storeId,
            UUID businessId,
            String tableName,
            String tableNumber,
            UUID qrCustomizationId,
            UUID performedBy
    ) {
        this(
                UUID.randomUUID(),
                "TABLE_QR_REGENERATION",
                Instant.now(),
                tableId,
                storeId,
                businessId,
                tableName,
                tableNumber,
                qrCustomizationId,
                performedBy
        );
    }
}
