package com.servemenu.businessservice.application.dto.response;

import com.servemenu.businessservice.domain.enums.TaxType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TaxResponse(
        UUID id,
        String name,
        TaxType type,
        BigDecimal dineInPercentage,
        BigDecimal takeOutPercentage,
        Boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {}
