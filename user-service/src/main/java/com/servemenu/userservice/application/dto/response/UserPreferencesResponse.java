package com.servemenu.userservice.application.dto.response;

import com.servemenu.userservice.domain.enums.Theme;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for user UI preferences
 * Note: Business notification settings are in business-service
 */
public record UserPreferencesResponse(
        UUID id,
        String dashboardLanguage,
        String timezone,
        Theme theme,
        Instant createdAt,
        Instant updatedAt
) {
}
