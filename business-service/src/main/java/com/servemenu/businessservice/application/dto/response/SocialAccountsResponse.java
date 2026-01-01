package com.servemenu.businessservice.application.dto.response;

import java.time.Instant;
import java.util.UUID;

public record SocialAccountsResponse(
        UUID id,
        String facebook,
        String twitter,
        String instagram,
        String snapchat,
        String pinterest,
        String foursquare,
        String tripadvisor,
        String zomato,
        String tiktok,
        Instant createdAt,
        Instant updatedAt
) {}
