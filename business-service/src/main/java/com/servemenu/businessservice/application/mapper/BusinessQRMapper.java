package com.servemenu.businessservice.application.mapper;

import com.servemenu.businessservice.application.dto.response.BusinessQRResponse;
import com.servemenu.businessservice.domain.model.Business;
import com.servemenu.businessservice.infrastructure.grpc.client.MediaServiceGrpcClient;
import com.servemenu.mediaservice.grpc.MediaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Business QR Mapper - Enriches business QR response with media URLs from Media Service
 * Same pattern as TableMapper for consistency
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BusinessQRMapper {

    private final MediaServiceGrpcClient mediaServiceGrpcClient;

    /**
     * Convert Business to BusinessQRResponse with QR URLs from Media Service
     */
    public BusinessQRResponse toQRResponse(Business business) {
        log.debug("Mapping business to QR response: businessId={}", business.getId());

        // Generate customer app URL
        String customerAppUrl = business.getCustomDomain() != null
                ? "https://" + business.getCustomDomain()
                : "https://menu.servemenu.com/" + business.getSlug();

        // Default null URLs
        String qrImageUrl = null;
        String qrThumbnailSmall = null;
        String qrThumbnailMedium = null;
        String qrThumbnailLarge = null;

        // Fetch QR URLs from Media Service if media ID exists
        if (business.getBusinessQrMediaId() != null) {
            Optional<MediaResponse> mediaOptional = mediaServiceGrpcClient.getMediaById(business.getBusinessQrMediaId());
            
            if (mediaOptional.isPresent()) {
                MediaResponse media = mediaOptional.get();
                
                log.debug("Media fetched for business QR: businessId={}, mediaId={}", 
                        business.getId(), business.getBusinessQrMediaId());
                
                // Map URLs with fallbacks (same as TableMapper)
                String originalUrl = media.getOriginalUrl();
                String largeUrl = media.getLargeUrl().isEmpty() ? originalUrl : media.getLargeUrl();
                String mediumUrl = media.getMediumUrl().isEmpty() ? media.getThumbnailUrl() : media.getMediumUrl();
                String smallUrl = media.getThumbnailUrl().isEmpty() ? originalUrl : media.getThumbnailUrl();
                
                qrImageUrl = originalUrl;           // Original (800x800)
                qrThumbnailSmall = smallUrl;        // Small (200x200)
                qrThumbnailMedium = mediumUrl;      // Medium (400x400)
                qrThumbnailLarge = largeUrl;        // Large (600x600)
                
                log.info("✅ Business QR URLs mapped successfully: businessId={}", business.getId());
            } else {
                log.warn("Media not found for business QR: businessId={}, mediaId={}", 
                        business.getId(), business.getBusinessQrMediaId());
            }
        } else {
            log.warn("Business has no QR media ID: businessId={}", business.getId());
        }

        return new BusinessQRResponse(
                qrImageUrl,
                qrThumbnailSmall,
                qrThumbnailMedium,
                qrThumbnailLarge,
                customerAppUrl,
                business.getSlug(),
                business.getCustomDomain()
        );
    }
}
