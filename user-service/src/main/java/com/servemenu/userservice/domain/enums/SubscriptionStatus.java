package com.servemenu.userservice.domain.enums;

public enum SubscriptionStatus {
    ACTIVE("Active"),
    TRIAL("Trial"),
    CANCELLED("Cancelled"),
    EXPIRED("Expired");

    private final String displayName;

    SubscriptionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
