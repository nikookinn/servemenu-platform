package com.servemenu.businessservice.application.dto.response;

import com.servemenu.businessservice.domain.enums.WifiType;

import java.time.Instant;
import java.util.UUID;

public record WiFiSettingsResponse(
        UUID id,
        UUID storeId,
        String wifiName, // "Guest WiFi", "Staff WiFi", etc.
        WifiType wifiType,
        String ssid,
        UUID qrCodeMediaId,
        QRCodeUrls qrCodeUrls,
        Boolean hasPassword,
        Boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
    public record QRCodeUrls(
            String largeUrl,    // 800x800 - For download
            String mediumUrl,
            String smallUrl     // 200x200 - For card thumbnail
    ) {}
}
