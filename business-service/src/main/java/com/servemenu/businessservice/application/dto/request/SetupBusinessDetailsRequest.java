package com.servemenu.businessservice.application.dto.request;

import com.servemenu.businessservice.domain.enums.BusinessType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SetupBusinessDetailsRequest(
        @NotBlank(message = "Business name is required")
        @Size(max = 255, message = "Business name must not exceed 255 characters")
        String businessName,

        @NotNull(message = "Business type is required")
        BusinessType businessType,

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be 3 characters (ISO 4217)")
        String currency,

        @NotEmpty(message = "At least one language is required")
        @Size(max = 10, message = "Maximum 10 languages allowed")
        List<@NotBlank @Size(min = 2, max = 5) String> supportedLanguages
) {}
