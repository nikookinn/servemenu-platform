package com.servemenu.businessservice.domain.enums;

import lombok.Getter;

@Getter
public enum SslStatus {
    PENDING("Pending"),
    ACTIVE("Active"),
    FAILED("Failed"),
    EXPIRED("Expired");

    private final String displayName;

    SslStatus(String displayName) {
        this.displayName = displayName;
    }
}
