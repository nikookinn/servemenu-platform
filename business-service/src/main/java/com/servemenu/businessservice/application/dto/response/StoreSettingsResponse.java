package com.servemenu.businessservice.application.dto.response;

import java.time.Instant;
import java.util.UUID;

public record StoreSettingsResponse(
        UUID id,
        Boolean enableDineIn,
        Boolean enableTakeaway,
        Boolean enablePickup,
        Boolean enableDelivery,
        Boolean enableGuestCheckout,
        Boolean allowSpecialInstructions,
        Boolean displayFullFoodName,
        Instant createdAt,
        Instant updatedAt
) {}
