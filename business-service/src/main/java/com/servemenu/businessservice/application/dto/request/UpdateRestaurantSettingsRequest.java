package com.servemenu.businessservice.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateRestaurantSettingsRequest(
        @Size(max = 500, message = "Logo URL must not exceed 500 characters")
        String logoUrl,

        @Size(max = 500, message = "Cover image URL must not exceed 500 characters")
        String coverImageUrl,

        @Size(max = 500, message = "Address must not exceed 500 characters")
        String address,

        @Email(message = "Email must be valid")
        String email,

        @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone number must be 10-15 digits")
        String phoneNumber,

        @Size(min = 1, max = 5, message = "Country code must be 1-5 characters")
        String countryCode,

        Boolean enableDefaultFoodImage
) {}
