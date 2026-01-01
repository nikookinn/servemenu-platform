package com.servemenu.businessservice.application.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for creating multiple tables at once
 */
public record BulkCreateTablesRequest(
        @NotEmpty(message = "Table names list cannot be empty")
        @Size(max = 100, message = "Cannot create more than 100 tables at once")
        List<@NotEmpty(message = "Table name cannot be empty") String> tableNames
) {
}
