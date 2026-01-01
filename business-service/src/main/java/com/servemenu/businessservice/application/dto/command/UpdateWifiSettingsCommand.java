package com.servemenu.businessservice.application.dto.command;

import com.servemenu.businessservice.domain.enums.WifiType;

public record UpdateWifiSettingsCommand(
        WifiType wifiType,
        String ssid,
        String password
) {}
