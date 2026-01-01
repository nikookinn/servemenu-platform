package com.servemenu.businessservice.application.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class QRUrlContext {
    private UUID storeId;
    private UUID tableId;
    private UUID businessId;
    private String businessSlug;
    private String qrUrl;
}
