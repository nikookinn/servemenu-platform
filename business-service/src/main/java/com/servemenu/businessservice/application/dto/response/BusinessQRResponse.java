package com.servemenu.businessservice.application.dto.response;

public record BusinessQRResponse(
        String qrImageUrl,           // Original QR image (800x800)
        String qrThumbnailSmall,     // Small thumbnail (200x200)
        String qrThumbnailMedium,    // Medium thumbnail (400x400)
        String qrThumbnailLarge,     // Large thumbnail (600x600)
        String customerAppUrl,       // Customer app URL
        String businessSlug,         // Business slug
        String customDomain          // Custom domain (if exists)
) {
}
