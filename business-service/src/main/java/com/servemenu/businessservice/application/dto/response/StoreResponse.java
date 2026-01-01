package com.servemenu.businessservice.application.dto.response;

import com.servemenu.businessservice.domain.enums.StoreStatus;

import java.time.Instant;
import java.util.UUID;

public record StoreResponse(
        UUID id,
        UUID businessId,
        String storeName,
        Boolean isDefault,
        String address,
        String phoneNumber,
        String countryCode,
        String timezone,
        UUID menuId,
        StoreStatus status,
        Instant createdAt,
        Instant updatedAt
) {}
