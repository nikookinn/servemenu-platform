package com.servemenu.businessservice.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateStoreRequest(
        @NotBlank(message = "Store name is required")
        @Size(max = 255, message = "Store name must not exceed 255 characters")
        String storeName,

        @Size(max = 500, message = "Address must not exceed 500 characters")
        String address,

        @Pattern(regexp = "^[0-9]{9,15}$", message = "Phone number must be 9-15 digits")
        String phoneNumber,

        @Size(min = 1, max = 5, message = "Country code must be 1-5 characters")
        String countryCode
) {}
