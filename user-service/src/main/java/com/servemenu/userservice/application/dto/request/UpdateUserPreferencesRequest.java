package com.servemenu.userservice.application.dto.request;

import com.servemenu.userservice.domain.enums.Theme;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating user UI preferences
 * Note: Notification settings are managed in business-service, not here
 */
public record UpdateUserPreferencesRequest(
        @Size(min = 2, max = 5, message = "Language code must be 2-5 characters")
        String dashboardLanguage,

        @Size(max = 50, message = "Timezone must be max 50 characters")
        String timezone,

        Theme theme
) {
}
