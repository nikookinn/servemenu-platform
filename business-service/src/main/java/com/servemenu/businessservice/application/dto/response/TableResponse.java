package com.servemenu.businessservice.application.dto.response;

import java.time.Instant;
import java.util.UUID;

public record TableResponse(
        UUID id,
        UUID storeId,
        String tableName,
        String tableNumber,
        UUID qrCodeMediaId,
        QRCodeUrls qrCodeUrls,
        Boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
    public record QRCodeUrls(
            String largeUrl,    // 800x800 - For download
            String mediumUrl,   // 500x500 - For preview  
            String smallUrl     // 500x500 - For card thumbnail
    ) {}
}
