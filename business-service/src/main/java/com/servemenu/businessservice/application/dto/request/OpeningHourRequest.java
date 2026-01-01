package com.servemenu.businessservice.application.dto.request;

import com.servemenu.businessservice.domain.enums.DayOfWeek;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OpeningHourRequest(
        @NotNull(message = "Day of week is required")
        DayOfWeek dayOfWeek,

        @NotNull(message = "isOpen flag is required")
        Boolean isOpen,

        @Valid
        List<TimeSlotRequest> timeSlots
) {}
