package com.servemenu.userservice.domain.event;

import com.servemenu.userservice.domain.enums.StoreAccessLevel;

import java.time.Instant;
import java.util.UUID;

public record StoreUserCreatedEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID userId,
        UUID keycloakUserId,
        UUID businessId,
        UUID storeId,
        StoreAccessLevel accessLevel,
        UUID createdByUserId
) {
    public StoreUserCreatedEvent(
            UUID userId,
            UUID keycloakUserId,
            UUID businessId,
            UUID storeId,
            StoreAccessLevel accessLevel,
            UUID createdByUserId
    ) {
        this(
                UUID.randomUUID(),
                "STORE_USER_CREATED",
                Instant.now(),
                userId,
                keycloakUserId,
                businessId,
                storeId,
                accessLevel,
                createdByUserId
        );
    }
}
