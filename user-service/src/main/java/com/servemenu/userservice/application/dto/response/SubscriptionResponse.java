package com.servemenu.userservice.application.dto.response;

import com.servemenu.userservice.domain.enums.SubscriptionPlan;
import com.servemenu.userservice.domain.enums.SubscriptionStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for subscription information
 */
public record SubscriptionResponse(
        UUID businessOwnerId,
        SubscriptionPlan currentPlan,
        SubscriptionStatus status,
        Instant subscriptionStartsAt,
        Instant subscriptionEndsAt,
        Instant trialEndsAt,
        PlanLimitsResponse limits
) {
    public record PlanLimitsResponse(
            int maxStores,
            int maxMenus,
            int maxCategoriesPerMenu,
            int maxItemsPerCategory,
            boolean wifiQrEnabled,
            boolean kitchenDisplayEnabled,
            boolean customDomainEnabled,
            boolean isUnlimitedStores,
            boolean isUnlimitedMenus
    ) {
    }
}

