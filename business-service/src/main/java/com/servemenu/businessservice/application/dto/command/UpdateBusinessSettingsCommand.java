package com.servemenu.businessservice.application.dto.command;

public record UpdateBusinessSettingsCommand (
        String logoUrl,
        String coverImageUrl,
        String address,
        String email,
        String phoneNumber,
        String countryCode,
        Boolean enableDefaultFoodImage) {}
