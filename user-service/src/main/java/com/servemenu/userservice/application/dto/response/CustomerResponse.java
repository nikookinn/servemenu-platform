package com.servemenu.userservice.application.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        UserResponse user,
        String phoneNumber,
        String countryCode,
        String preferredLanguage,
        UUID createdByBusinessId,
        Integer loyaltyPoints,
        Instant createdAt,
        Instant updatedAt
) {
}
