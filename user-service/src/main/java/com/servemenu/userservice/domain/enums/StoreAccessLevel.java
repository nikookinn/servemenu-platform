package com.servemenu.userservice.domain.enums;

public enum StoreAccessLevel {
    STORE_ADMIN("Store Administrator"),
    STORE_USER("Store User");

    private final String displayName;

    StoreAccessLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}