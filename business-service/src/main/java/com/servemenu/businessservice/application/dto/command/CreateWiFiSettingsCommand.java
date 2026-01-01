package com.servemenu.businessservice.application.dto.command;

import com.servemenu.businessservice.domain.enums.WifiType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateWiFiSettingsCommand(
        @NotBlank(message = "WiFi name is required")
        String wifiName, // e.g., "Guest WiFi", "Staff WiFi"

        @NotBlank(message = "SSID is required")
        String ssid,

        String password, // Optional for open networks

        @NotNull(message = "WiFi type is required")
        WifiType wifiType
) {
}
