package com.servemenu.businessservice.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record UpdateBusinessSettingsRequest(
        @NotBlank(message = "Business name is required")
        @Size(max = 255, message = "Business name must not exceed 255 characters")
        String businessName,

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter code")
        String currency,

        @NotEmpty(message = "At least one supported language is required")
        List<String> supportedLanguages,

        @NotBlank(message = "Default language is required")
        @Size(max = 5, message = "Language code must not exceed 5 characters")
        String defaultLanguage,

        UUID logoMediaId,

        UUID coverImageMediaId,

        @Size(max = 500, message = "Address must not exceed 500 characters")
        String address,

        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @Size(max = 20, message = "Phone number must not exceed 20 characters")
        String phoneNumber,

        @Size(max = 5, message = "Country code must not exceed 5 characters")
        String countryCode
) {
}
