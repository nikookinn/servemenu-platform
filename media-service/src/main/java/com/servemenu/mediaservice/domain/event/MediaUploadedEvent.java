package com.servemenu.mediaservice.domain.event;

import com.servemenu.mediaservice.domain.enums.MediaType;

import java.time.Instant;
import java.util.UUID;

public record MediaUploadedEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID mediaId,
        UUID entityId,  // tableId
        UUID storeId,
        MediaType mediaType,
        String originalUrl,
        String largeUrl,
        String thumbnailUrl
) {
    public MediaUploadedEvent(
            UUID mediaId,
            UUID entityId,
            UUID storeId,
            MediaType mediaType,
            String originalUrl,
            String largeUrl,
            String thumbnailUrl
    ) {
        this(
                UUID.randomUUID(),
                "MEDIA_UPLOADED",
                Instant.now(),
                mediaId,
                entityId,
                storeId,
                mediaType,
                originalUrl,
                largeUrl,
                thumbnailUrl
        );
    }
}
