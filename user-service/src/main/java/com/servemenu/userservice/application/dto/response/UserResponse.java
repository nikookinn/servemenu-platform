package com.servemenu.userservice.application.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.servemenu.userservice.domain.enums.AccountStatus;
import com.servemenu.userservice.domain.enums.UserType;

import java.time.Instant;
import java.util.UUID;

/**
 * Base user response with only essential user information
 * For role-specific data, use dedicated endpoints:
 * - /api/v1/business-owners/me → BusinessOwnerResponse
 * - /api/v1/customers/me → CustomerResponse
 * - /api/v1/store-users/me → StoreUserResponse
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(
        UUID id,
        UUID keycloakUserId,
        String email,
        String firstName,
        String lastName,
        Boolean emailVerified,
        UserType userType,
        AccountStatus accountStatus,
        Instant createdAt,
        Instant updatedAt
) {
}
