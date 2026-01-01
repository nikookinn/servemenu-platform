package com.servemenu.userservice.domain.event;

import java.time.Instant;

/**
 * Event representing email verification from Keycloak.
 * Uses a minimal payload for efficient processing.
 */
public record EmailVerifiedEvent(
        String eventId,
        String eventType,
        Instant timestamp,
        String userId,
        boolean emailVerified
) {
    /**
     * Factory method to create an EmailVerifiedEvent from JSON payload
     */
    public static EmailVerifiedEvent fromPayload(
            String eventId,
            String eventType,
            String timestamp,
            String userId,
            boolean emailVerified
    ) {
        return new EmailVerifiedEvent(
                eventId,
                eventType,
                Instant.parse(timestamp),
                userId,
                emailVerified
        );
    }
}
