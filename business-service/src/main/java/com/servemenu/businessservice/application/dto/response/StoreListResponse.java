package com.servemenu.businessservice.application.dto.response;

import com.servemenu.businessservice.domain.enums.StoreStatus;

import java.time.Instant;
import java.util.UUID;

public record StoreListResponse(
        UUID id,
        String storeName,
        Boolean isDefault,
        String address,
        UUID menuId,
        StoreStatus status,
        int tableCount,
        Instant createdAt
) {}
