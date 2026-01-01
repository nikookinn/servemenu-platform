package com.servemenu.businessservice.application.dto.response;

import java.time.Instant;
import java.util.UUID;

public record LocationDetailsResponse(
        UUID id,
        Boolean isEnabled,
        Double latitude,
        Double longitude,
        Integer radiusInMeters,
        Instant createdAt,
        Instant updatedAt
) {}
