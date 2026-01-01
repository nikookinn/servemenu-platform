package com.servemenu.businessservice.domain.enums;

import lombok.Getter;

@Getter
public enum TaxType {
    INCLUDE_TO_PRICE("Include to Price"),
    NOT_INCLUDE_TO_PRICE("Not Include to Price");

    private final String displayName;

    TaxType(String displayName) {
        this.displayName = displayName;
    }

}
