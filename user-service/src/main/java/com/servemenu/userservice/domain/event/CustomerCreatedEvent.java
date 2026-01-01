package com.servemenu.userservice.domain.event;

import java.time.Instant;
import java.util.UUID;

public record CustomerCreatedEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID userId,
        UUID keycloakUserId,
        String email,
        UUID createdByBusinessId
) {
    public CustomerCreatedEvent(
            UUID userId,
            UUID keycloakUserId,
            String email,
            UUID createdByBusinessId
    ) {
        this(
                UUID.randomUUID(),
                "CUSTOMER_CREATED",
                Instant.now(),
                userId,
                keycloakUserId,
                email,
                createdByBusinessId
        );
    }
}
