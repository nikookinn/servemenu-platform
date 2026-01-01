package com.servemenu.businessservice.application.dto.command;

public record UpdateLocationDetailsCommand(
        Boolean isEnabled,
        Double latitude,
        Double longitude,
        Integer radiusInMeters
) {}
