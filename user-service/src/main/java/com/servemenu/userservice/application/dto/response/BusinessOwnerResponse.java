package com.servemenu.userservice.application.dto.response;

import com.servemenu.userservice.domain.enums.SubscriptionPlan;
import com.servemenu.userservice.domain.enums.SubscriptionStatus;

import java.time.Instant;
import java.util.UUID;

public record BusinessOwnerResponse(
        UUID id,
        UserResponse user,
        Boolean onboardingCompleted,
        UUID businessId,
        SubscriptionPlan subscriptionPlan,
        SubscriptionStatus subscriptionStatus,
        Instant trialEndsAt,
        Instant subscriptionStartsAt,
        Instant subscriptionEndsAt,
        Instant createdAt,
        Instant updatedAt
) {
}
