package com.servemenu.userservice.domain.event;

import java.time.Instant;
import java.util.List;

public record UserRegisteredEvent(
        String eventId,
        String eventType,
        Instant timestamp,
        String realm,
        String clientId,
        UserInfo user
) {
    public record UserInfo(
            String id,
            String username,
            String email,
            String firstName,
            String lastName,
            boolean emailVerified,
            boolean enabled,
            List<String> roles
    ) {}
}
