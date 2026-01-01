package com.servemenu.businessservice.application.dto.command;

public record UpdateNotificationSettingsCommand(
        String orderNotificationSound,
        Boolean orderNotificationEnabled,
        String feedbackNotificationSound,
        Boolean feedbackNotificationEnabled,
        String hotActionNotificationSound,
        Boolean hotActionNotificationEnabled
) {}
