package com.servemenu.businessservice.application.dto.response;

import java.time.Instant;
import java.util.UUID;

public record OrderSettingsResponse(
        UUID id,
        Boolean enableCustomerTip,
        Boolean enableCancelOrder,
        String invoiceIdPrefix,
        Instant createdAt,
        Instant updatedAt
) {}
