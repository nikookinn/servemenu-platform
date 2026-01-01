package com.servemenu.businessservice.application.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateNotificationSettingsRequest(
        @Size(max = 100, message = "Sound name must not exceed 100 characters")
        String orderNotificationSound,

        Boolean orderNotificationEnabled,

        @Size(max = 100, message = "Sound name must not exceed 100 characters")
        String feedbackNotificationSound,

        Boolean feedbackNotificationEnabled,

        @Size(max = 100, message = "Sound name must not exceed 100 characters")
        String hotActionNotificationSound,

        Boolean hotActionNotificationEnabled
) {}
