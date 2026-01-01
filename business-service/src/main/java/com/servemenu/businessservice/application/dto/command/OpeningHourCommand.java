package com.servemenu.businessservice.application.dto.command;


import com.servemenu.businessservice.domain.enums.DayOfWeek;

import java.util.List;

public record OpeningHourCommand(
        DayOfWeek dayOfWeek,
        Boolean isOpen,
        List<TimeSlotCommand> timeSlots
) {}
