package com.servemenu.businessservice.application.dto.command;

import com.servemenu.businessservice.domain.enums.TaxType;

import java.math.BigDecimal;

public record CreateTaxCommand(
        String name,
        TaxType type,
        BigDecimal dineInPercentage,
        BigDecimal takeOutPercentage
) {}
