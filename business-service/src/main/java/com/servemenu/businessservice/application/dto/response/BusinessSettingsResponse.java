package com.servemenu.businessservice.application.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BusinessSettingsResponse(
        UUID id,
        UUID businessId,
        String businessName,
        String slug,
        String currency,
        List<String> supportedLanguages,
        String defaultLanguage,
        UUID logoMediaId,
        String logoUrl,
        UUID coverImageMediaId,
        String coverImageUrl,
        String address,
        String email,
        String phoneNumber,
        String countryCode,
        Boolean enableDefaultFoodImage,
        Instant createdAt,
        Instant updatedAt
) {
}
