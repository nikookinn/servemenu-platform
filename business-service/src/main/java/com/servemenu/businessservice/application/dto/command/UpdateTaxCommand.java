package com.servemenu.businessservice.application.dto.command;

import com.servemenu.businessservice.domain.enums.TaxType;

import java.math.BigDecimal;

public record UpdateTaxCommand(
        String name,
        TaxType type,
        BigDecimal dineInPercentage,
        BigDecimal takeOutPercentage,
        Boolean isActive
) {}
