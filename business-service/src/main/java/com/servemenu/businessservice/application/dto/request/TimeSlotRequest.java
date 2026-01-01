package com.servemenu.businessservice.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TimeSlotRequest(
        @NotBlank(message = "Open time is required")
        @Pattern(regexp = "^([0-1][0-9]|2[0-3]):[0-5][0-9]$", message = "Time must be in HH:mm format")
        String openTime,

        @NotBlank(message = "Close time is required")
        @Pattern(regexp = "^([0-1][0-9]|2[0-3]):[0-5][0-9]$", message = "Time must be in HH:mm format")
        String closeTime
) {}
