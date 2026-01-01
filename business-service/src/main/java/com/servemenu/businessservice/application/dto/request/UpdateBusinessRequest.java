package com.servemenu.businessservice.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateBusinessRequest(
        @Size(max = 255, message = "Business name must not exceed 255 characters")
        String businessName,

        @Size(max = 10, message = "Maximum 10 languages allowed")
        List<@NotBlank @Size(min = 2, max = 5) String> supportedLanguages
) {
}
