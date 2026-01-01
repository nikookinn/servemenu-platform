package com.servemenu.businessservice.application.dto.response;

import java.time.Instant;
import java.util.UUID;

public record NotificationSettingsResponse(
        UUID id,
        String orderNotificationSound,
        Boolean orderNotificationEnabled,
        String feedbackNotificationSound,
        Boolean feedbackNotificationEnabled,
        String hotActionNotificationSound,
        Boolean hotActionNotificationEnabled,
        Instant createdAt,
        Instant updatedAt
) {}
