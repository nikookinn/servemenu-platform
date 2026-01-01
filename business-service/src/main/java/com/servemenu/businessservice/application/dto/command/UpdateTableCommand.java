package com.servemenu.businessservice.application.dto.command;

public record UpdateTableCommand(
        String tableName,
        Boolean isActive
) {}
