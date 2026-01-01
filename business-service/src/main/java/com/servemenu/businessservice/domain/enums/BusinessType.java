package com.servemenu.businessservice.domain.enums;

import lombok.Getter;

@Getter
public enum BusinessType {
    HOTEL("Hotel"),
    CAFE("Cafe"),
    FOOD_TRUCK("Food Truck"),
    QSR("Quick Service Restaurant"),
    PUB_BAR("Pub/Bar");

    private final String displayName;

    BusinessType(String displayName) {
        this.displayName = displayName;
    }

}
