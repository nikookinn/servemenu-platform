package com.servemenu.businessservice.application.dto.response;

import java.util.List;

/**
 * Response DTO for bulk table creation
 */
public record BulkCreateTablesResponse(
        List<TableResponse> createdTables,
        int totalCreated,
        String message
) {
    public static BulkCreateTablesResponse of(List<TableResponse> tables) {
        return new BulkCreateTablesResponse(
                tables,
                tables.size(),
                String.format("%d table(s) created successfully", tables.size())
        );
    }
}
