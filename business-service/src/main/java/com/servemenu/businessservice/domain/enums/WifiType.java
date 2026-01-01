package com.servemenu.businessservice.domain.enums;

import lombok.Getter;

@Getter
public enum WifiType {
    WPA("WPA/WPA2"),
    WEB("Web Authentication"),
    NO_ENCRYPTION("No Encryption");

    private final String displayName;

    WifiType(String displayName) {
        this.displayName = displayName;
    }

}
