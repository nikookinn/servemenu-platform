package com.servemenu.businessservice.application.dto.command;

public record CreateStoreCommand(
        String storeName,
        String address,
        String phoneNumber,
        String countryCode
) {}
