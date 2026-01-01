package com.servemenu.businessservice.application.dto.request;

import com.servemenu.businessservice.domain.enums.TaxType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateTaxRequest(
        @Size(max = 100, message = "Tax name must not exceed 100 characters")
        String name,

        TaxType type,

        @DecimalMin(value = "0.0", message = "Percentage must be >= 0")
        @DecimalMax(value = "100.0", message = "Percentage must be <= 100")
        @Digits(integer = 3, fraction = 2, message = "Invalid percentage format")
        BigDecimal dineInPercentage,

        @DecimalMin(value = "0.0", message = "Percentage must be >= 0")
        @DecimalMax(value = "100.0", message = "Percentage must be <= 100")
        @Digits(integer = 3, fraction = 2, message = "Invalid percentage format")
        BigDecimal takeOutPercentage,

        Boolean isActive
) {}
