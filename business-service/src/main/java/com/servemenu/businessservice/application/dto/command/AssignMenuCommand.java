package com.servemenu.businessservice.application.dto.command;

import java.util.UUID;

public record AssignMenuCommand(
        UUID menuId
) {}
