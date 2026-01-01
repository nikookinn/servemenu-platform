package com.servemenu.businessservice.domain.enums;

import lombok.Getter;

@Getter
public enum DomainVerificationStatus {
    PENDING("Pending Verification"),
    VERIFIED("Verified"),
    FAILED("Verification Failed");

    private final String displayName;

    DomainVerificationStatus(String displayName) {
        this.displayName = displayName;
    }

}
