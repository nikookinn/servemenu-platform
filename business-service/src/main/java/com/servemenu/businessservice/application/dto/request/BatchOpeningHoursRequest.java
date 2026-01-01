package com.servemenu.businessservice.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BatchOpeningHoursRequest(
        @NotNull(message = "Opening hours are required")
        @Size(min = 1, max = 7, message = "Must provide 1-7 days")
        @Valid
        List<OpeningHourRequest> openingHours
) {}
