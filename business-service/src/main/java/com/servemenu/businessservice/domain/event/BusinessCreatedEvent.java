package com.servemenu.businessservice.domain.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BusinessCreatedEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID businessId,
        UUID businessOwnerId,
        UUID keycloakUserId,
        String businessName,
        String slug,
        String currency,
        List<String> supportedLanguages
) {
    public BusinessCreatedEvent(
            UUID businessId,
            UUID businessOwnerId,
            UUID keycloakUserId,
            String businessName,
            String slug,
            String currency,
            List<String> supportedLanguages
    ) {
        this(
                UUID.randomUUID(),
                "BUSINESS_CREATED",
                Instant.now(),
                businessId,
                businessOwnerId,
                keycloakUserId,
                businessName,
                slug,
                currency,
                supportedLanguages
        );
    }
}
