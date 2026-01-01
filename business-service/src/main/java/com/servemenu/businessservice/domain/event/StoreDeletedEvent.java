package com.servemenu.businessservice.domain.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StoreDeletedEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID storeId,
        UUID businessId,
        List<UUID> tableIds,           // Batch table IDs
        List<UUID> qrCodeMediaIds      // Batch QR media IDs to cleanup
) {
    public StoreDeletedEvent(UUID storeId, UUID businessId, List<UUID> tableIds, List<UUID> qrCodeMediaIds) {
        this(
                UUID.randomUUID(),
                "STORE_DELETED",
                Instant.now(),
                storeId,
                businessId,
                tableIds,
                qrCodeMediaIds
        );
    }
}
