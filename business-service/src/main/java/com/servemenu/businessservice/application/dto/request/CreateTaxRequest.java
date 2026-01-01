package com.servemenu.businessservice.application.dto.request;

import com.servemenu.businessservice.domain.enums.TaxType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateTaxRequest(
        @NotBlank(message = "Tax name is required")
        @Size(max = 100, message = "Tax name must not exceed 100 characters")
        String name,

        @NotNull(message = "Tax type is required")
        TaxType type,

        @NotNull(message = "Dine-in percentage is required")
        @DecimalMin(value = "0.0", message = "Percentage must be >= 0")
        @DecimalMax(value = "100.0", message = "Percentage must be <= 100")
        @Digits(integer = 3, fraction = 2, message = "Invalid percentage format")
        BigDecimal dineInPercentage,

        @NotNull(message = "Take-out percentage is required")
        @DecimalMin(value = "0.0", message = "Percentage must be >= 0")
        @DecimalMax(value = "100.0", message = "Percentage must be <= 100")
        @Digits(integer = 3, fraction = 2, message = "Invalid percentage format")
        BigDecimal takeOutPercentage
) {}
