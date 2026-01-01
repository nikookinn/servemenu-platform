package com.servemenu.businessservice.application.mapper;

import com.servemenu.businessservice.application.dto.response.WiFiSettingsResponse;
import com.servemenu.businessservice.domain.model.WifiSettings;
import com.servemenu.businessservice.infrastructure.grpc.client.MediaServiceGrpcClient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public abstract class WiFiSettingsMapper {

    @Autowired
    protected MediaServiceGrpcClient mediaServiceGrpcClient;

    @Mapping(target = "storeId", source = "store.id")
    @Mapping(target = "hasPassword", expression = "java(wifiSettings.getPassword() != null && !wifiSettings.getPassword().isEmpty())")
    @Mapping(target = "qrCodeUrls", expression = "java(enrichWithQRUrls(wifiSettings))")
    public abstract WiFiSettingsResponse toResponse(WifiSettings wifiSettings);

    /**
     * Enrich WiFi settings with QR code URLs from Media Service
     */
    protected WiFiSettingsResponse.QRCodeUrls enrichWithQRUrls(WifiSettings wifiSettings) {
        if (wifiSettings.getQrCodeMediaId() == null) {
            return null;
        }

        return mediaServiceGrpcClient.getMediaById(wifiSettings.getQrCodeMediaId())
                .map(media -> {
                    String largeUrl = media.getLargeUrl() != null && !media.getLargeUrl().isEmpty() 
                            ? media.getLargeUrl() 
                            : null;
                    String mediumUrl = media.getMediumUrl() != null && !media.getMediumUrl().isEmpty() 
                            ? media.getMediumUrl() 
                            : null;
                    String smallUrl = media.getThumbnailUrl() != null && !media.getThumbnailUrl().isEmpty() 
                            ? media.getThumbnailUrl() 
                            : null;

                    return new WiFiSettingsResponse.QRCodeUrls(
                            largeUrl,   // Large: 800x800 - For download
                            mediumUrl,  // Medium: 500x500 - For preview
                            smallUrl    // Small: 200x200 - For thumbnail
                    );
                })
                .orElse(null);
    }
}
