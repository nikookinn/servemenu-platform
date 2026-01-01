package com.servemenu.userservice.application.dto.request;

import com.servemenu.userservice.domain.enums.SubscriptionPlan;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for upgrading subscription plan
 * Typically triggered by payment-service after successful payment
 */
public record UpgradeSubscriptionRequest(
        @NotNull(message = "Subscription plan is required")
        SubscriptionPlan subscriptionPlan
) {
}

