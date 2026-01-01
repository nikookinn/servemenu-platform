package com.servemenu.userservice.application.dto.response;

import com.servemenu.userservice.domain.enums.StoreAccessLevel;

import java.time.Instant;
import java.util.UUID;

public record StoreUserResponse(
        UUID id,
        UserResponse user,
        UUID businessId,
        UUID storeId,
        StoreAccessLevel accessLevel,
        Boolean isActive,
        UUID createdByUserId,
        Instant createdAt
) {
}
