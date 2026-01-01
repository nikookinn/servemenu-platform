package com.servemenu.userservice.domain.event;

import java.time.Instant;
import java.util.UUID;

public record OnboardingCompletedEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID userId,
        UUID keycloakUserId
) {
    public OnboardingCompletedEvent(UUID userId, UUID keycloakUserId) {
        this(
                UUID.randomUUID(),
                "ONBOARDING_COMPLETED",
                Instant.now(),
                userId,
                keycloakUserId
        );
    }
}
