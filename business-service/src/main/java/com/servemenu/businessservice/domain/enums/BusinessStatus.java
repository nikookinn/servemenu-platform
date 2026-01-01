package com.servemenu.businessservice.domain.enums;

import lombok.Getter;

@Getter
public enum BusinessStatus {
    DRAFT("Draft"),
    ACTIVE("Active"),
    SUSPENDED("Suspended"),
    DELETED("Deleted");

    private final String displayName;

    BusinessStatus(String displayName) {
        this.displayName = displayName;
    }

}
