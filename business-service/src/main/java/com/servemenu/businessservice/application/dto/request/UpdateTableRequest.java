package com.servemenu.businessservice.application.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateTableRequest(
        @Size(max = 100, message = "Table name must not exceed 100 characters")
        String tableName,

        Boolean isActive
) {}
