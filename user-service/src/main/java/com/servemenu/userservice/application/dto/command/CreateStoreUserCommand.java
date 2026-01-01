package com.servemenu.userservice.application.dto.command;

import com.servemenu.userservice.domain.enums.StoreAccessLevel;

import java.util.UUID;

public record CreateStoreUserCommand(
        String email,
        String firstName,
        String lastName,
        String password,
        UUID businessId,
        UUID storeId,
        StoreAccessLevel accessLevel,
        UUID createdByUserId
) {
}
