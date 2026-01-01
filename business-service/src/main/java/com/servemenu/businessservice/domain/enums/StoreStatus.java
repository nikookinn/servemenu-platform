package com.servemenu.businessservice.domain.enums;

import lombok.Getter;

@Getter
public enum StoreStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    DELETED("Deleted");

    private final String displayName;

    StoreStatus(String displayName) {
        this.displayName = displayName;
    }
}
