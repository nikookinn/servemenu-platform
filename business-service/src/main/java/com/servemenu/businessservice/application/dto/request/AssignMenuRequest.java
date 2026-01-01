package com.servemenu.businessservice.application.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignMenuRequest(
        @NotNull(message = "Menu ID is required")
        UUID menuId
) {}
