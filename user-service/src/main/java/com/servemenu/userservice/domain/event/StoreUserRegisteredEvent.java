package com.servemenu.userservice.domain.event;

import java.time.Instant;
import java.util.UUID;

public record StoreUserRegisteredEvent(
        String eventId,
        String eventType,
        Instant timestamp,
        String realm,
        UUID keycloakUserId,
        String email,
        String firstName,
        String lastName,
        UUID businessId,
        UUID storeId,
        String accessLevel,
        UUID createdByUserId
) {
}
