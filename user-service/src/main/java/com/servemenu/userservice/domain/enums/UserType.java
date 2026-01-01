package com.servemenu.userservice.domain.enums;

public enum UserType {
    BUSINESS_OWNER("Business Owner"),
    STORE_USER("Store User"),
    CUSTOMER("Customer");

    private final String displayName;

    UserType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
