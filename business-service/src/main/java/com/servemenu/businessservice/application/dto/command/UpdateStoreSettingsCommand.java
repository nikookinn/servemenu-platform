package com.servemenu.businessservice.application.dto.command;

public record UpdateStoreSettingsCommand(
        Boolean enableDineIn,
        Boolean enableTakeaway,
        Boolean enablePickup,
        Boolean enableDelivery,
        Boolean enableGuestCheckout,
        Boolean allowSpecialInstructions,
        Boolean displayFullFoodName
) {}
