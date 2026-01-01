package com.servemenu.businessservice.application.dto.response;

import com.servemenu.businessservice.domain.enums.DayOfWeek;
import com.servemenu.businessservice.domain.model.TimeSlot;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OpeningHoursResponse(
        UUID id,
        DayOfWeek dayOfWeek,
        Boolean isOpen,
        List<TimeSlot> timeSlots,
        Instant createdAt,
        Instant updatedAt
) {}
