package com.servemenu.businessservice.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain Event: WiFi Settings Deleted
 * Published when a WiFi network is soft deleted
 * Triggers QR code cleanup in Media Service
 */
public record WifiSettingsDeletedEvent(
        UUID eventId,
        Instant timestamp,
        UUID wifiSettingsId,
        UUID storeId,
        UUID qrCodeMediaId  // QR code to be deleted from Media Service
) {
    public WifiSettingsDeletedEvent(UUID wifiSettingsId, UUID storeId, UUID qrCodeMediaId) {
        this(
                UUID.randomUUID(),
                Instant.now(),
                wifiSettingsId,
                storeId,
                qrCodeMediaId
        );
    }
}
