package com.servemenu.businessservice.application.dto.response;

import com.servemenu.businessservice.domain.enums.StoreStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StoreDetailResponse(
        UUID id,
        UUID businessId,
        String storeName,
        Boolean isDefault,
        String address,
        String phoneNumber,
        String countryCode,
        String timezone,
        UUID menuId,
        StoreStatus status,
        StoreSettingsResponse settings,
        SocialAccountsResponse socialAccounts,
        List<WiFiSettingsResponse> wifiSettings,
        LocationDetailsResponse locationDetails,
        List<OpeningHoursResponse> openingHours,
        int tableCount,
        Instant createdAt,
        Instant updatedAt
) {}
