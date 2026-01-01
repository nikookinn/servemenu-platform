package com.servemenu.businessservice.application.dto.command;

import com.servemenu.businessservice.domain.enums.BusinessType;

import java.util.List;
import java.util.UUID;

public record SetupBusinessDetailsCommand(
        UUID keycloakUserId,
        String businessName,
        BusinessType businessType,
        String currency,
        List<String> supportedLanguages
) {}
