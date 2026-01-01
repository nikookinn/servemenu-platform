package com.servemenu.businessservice.application.dto.command;

public record UpdateStoreCommand(
        String storeName,
        String address,
        String phoneNumber,
        String countryCode
) {}
