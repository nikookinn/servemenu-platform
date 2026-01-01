package com.servemenu.businessservice.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTableRequest(
        @NotBlank(message = "Table name is required")
        @Size(max = 100, message = "Table name must not exceed 100 characters")
        String tableName
) {}
