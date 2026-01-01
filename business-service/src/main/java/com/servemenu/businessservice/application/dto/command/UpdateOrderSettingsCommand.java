package com.servemenu.businessservice.application.dto.command;

public record UpdateOrderSettingsCommand(
        Boolean enableCustomerTip,
        Boolean enableCancelOrder,
        String invoiceIdPrefix
) {}
