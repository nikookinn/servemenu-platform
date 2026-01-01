package com.servemenu.businessservice.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterCustomDomainRequest(
        @NotBlank(message = "Domain is required")
        @Pattern(
                regexp = "^(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$",
                message = "Invalid domain format"
        )
        String domain
) {}
