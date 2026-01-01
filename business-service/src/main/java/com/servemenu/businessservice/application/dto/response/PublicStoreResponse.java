package com.servemenu.businessservice.application.dto.response;

import java.util.UUID;

public record PublicStoreResponse(
        UUID id,
        String storeName,
        String address,
        String email,
        String phoneNumber,
        Boolean isDefault
) {}
