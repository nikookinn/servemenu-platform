package com.servemenu.businessservice.application.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateOrderSettingsRequest(
        Boolean enableCustomerTip,

        Boolean enableCancelOrder,

        @Size(min = 1, max = 20, message = "Invoice ID prefix must be 1-20 characters")
        String invoiceIdPrefix
) {}
