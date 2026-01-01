package com.servemenu.mediaservice.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a QR code is generated for a table
 * This event is consumed by media-service to store QR image
 */
public record QRGeneratedEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID tableId,
        UUID storeId,
        UUID businessId,
        String tableName,
        String tableNumber,
        String qrUrl,
        String qrImageBase64
) {
    public QRGeneratedEvent(
            UUID tableId,
            UUID storeId,
            UUID businessId,
            String tableName,
            String tableNumber,
            String qrUrl,
            String qrImageBase64
    ) {
        this(
                UUID.randomUUID(),
                "QR_GENERATED",
                Instant.now(),
                tableId,
                storeId,
                businessId,
                tableName,
                tableNumber,
                qrUrl,
                qrImageBase64
        );
    }
}
