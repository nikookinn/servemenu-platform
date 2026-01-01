package com.servemenu.businessservice.domain.event;

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
) {}
