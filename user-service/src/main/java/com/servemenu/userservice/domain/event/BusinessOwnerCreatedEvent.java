package com.servemenu.userservice.domain.event;

import java.time.Instant;
import java.util.UUID;

public record BusinessOwnerCreatedEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID userId,
        UUID keycloakUserId,
        String email,
        String firstName,
        String lastName
) {
    public BusinessOwnerCreatedEvent(
            UUID userId,
            UUID keycloakUserId,
            String email,
            String firstName,
            String lastName
    ) {
        this(
                UUID.randomUUID(),
                "BUSINESS_OWNER_CREATED",
                Instant.now(),
                userId,
                keycloakUserId,
                email,
                firstName,
                lastName
        );
    }
}
