package com.servemenu.businessservice.application.dto.command;

import java.util.List;

public record BatchOpeningHoursCommand(
        List<OpeningHourCommand> openingHours
) {}
