package com.servemenu.businessservice.application.dto.command;

public record TimeSlotCommand(
        String openTime,
        String closeTime
) {}
