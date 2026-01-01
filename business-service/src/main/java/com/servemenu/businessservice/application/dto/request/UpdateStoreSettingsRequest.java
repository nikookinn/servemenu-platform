package com.servemenu.businessservice.application.dto.request;

public record UpdateStoreSettingsRequest(
        Boolean enableDineIn,
        Boolean enableTakeaway,
        Boolean enablePickup,
        Boolean enableDelivery,
        Boolean enableGuestCheckout,
        Boolean allowSpecialInstructions,
        Boolean displayFullFoodName
) {}
