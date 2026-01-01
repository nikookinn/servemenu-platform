package com.servemenu.businessservice.domain.event;

import com.servemenu.businessservice.domain.enums.QRType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Unified QR Generation Event
 * Supports all QR types: BUSINESS, TABLE, WIFI
 */
public record QRGenerationRequestedEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID storeId,
        UUID businessId,
        QRType qrType,
        String targetUrl,
        Map<String, Object> metadata
) {
    public QRGenerationRequestedEvent(
            UUID storeId,
            UUID businessId,
            QRType qrType,
            String targetUrl,
            Map<String, Object> metadata
    ) {
        this(
                UUID.randomUUID(),
                "QR_GENERATION_REQUESTED",
                Instant.now(),
                storeId,
                businessId,
                qrType,
                targetUrl,
                metadata
        );
    }
}
