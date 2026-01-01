package com.servemenu.businessservice.application.dto.request;

import jakarta.validation.constraints.*;

public record UpdateLocationDetailsRequest(
        @NotNull(message = "isEnabled flag is required")
        Boolean isEnabled,

        @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
        @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
        Double latitude,

        @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
        @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
        Double longitude,

        @Min(value = 1, message = "Radius must be at least 1 meter")
        @Max(value = 10000, message = "Radius must not exceed 10000 meters")
        Integer radiusInMeters
) {}
