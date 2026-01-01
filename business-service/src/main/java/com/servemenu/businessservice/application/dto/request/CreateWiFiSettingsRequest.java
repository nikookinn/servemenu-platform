package com.servemenu.businessservice.application.dto.request;

import com.servemenu.businessservice.domain.enums.WifiType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateWiFiSettingsRequest(
        @NotNull(message = "WiFi name is required")
        String wifiName,
        @NotNull(message = "WiFi type is required")
        WifiType wifiType,

        @NotBlank(message = "SSID is required")
        @Size(max = 32, message = "SSID must not exceed 32 characters")
        String ssid,

        @Size(max = 63, message = "Password must not exceed 63 characters")
        String password
) {}
