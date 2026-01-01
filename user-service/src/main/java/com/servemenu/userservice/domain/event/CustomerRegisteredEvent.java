package com.servemenu.userservice.domain.event;

import java.time.Instant;
import java.util.UUID;

public record CustomerRegisteredEvent(
        String eventId,
        String eventType,
        Instant timestamp,
        String realm,
        UUID keycloakUserId,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        String countryCode,
        UUID createdByBusinessId
) {
}
