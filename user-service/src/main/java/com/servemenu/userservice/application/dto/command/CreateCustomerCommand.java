package com.servemenu.userservice.application.dto.command;

import java.util.UUID;

public record CreateCustomerCommand(
        String email,
        String firstName,
        String lastName,
        String password,
        String phoneNumber,
        String countryCode,
        UUID createdByBusinessId
) {
}
