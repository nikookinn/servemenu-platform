package com.servemenu.businessservice.application.dto.response;

import com.servemenu.businessservice.domain.enums.BusinessStatus;
import com.servemenu.businessservice.domain.enums.BusinessType;
import com.servemenu.businessservice.domain.enums.SubscriptionPlan;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BusinessResponse(
        UUID id,
        UUID businessOwnerId,
        UUID keycloakUserId,
        String businessName,
        BusinessType businessType,
        String currency,
        List<String> supportedLanguages,
        String defaultLanguage,
        String slug,
        String customDomain,
        Boolean customDomainVerified,
        Boolean isOnboardingCompleted,
        BusinessStatus status,
        SubscriptionPlan subscriptionPlan,
        Instant createdAt,
        Instant updatedAt
) {}
