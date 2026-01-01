package com.servemenu.businessservice.application.dto.response;

import com.servemenu.businessservice.domain.enums.BusinessType;

import java.util.List;
import java.util.UUID;

public record PublicBusinessResponse(
        UUID id,
        String businessName,
        BusinessType businessType,
        String slug,
        String logoUrl,
        String coverImageUrl,
        String address,
        String email,
        String phoneNumber,
        List<PublicStoreResponse> stores
) {}
