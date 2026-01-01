package com.servemenu.businessservice.application.service;

import com.servemenu.businessservice.application.dto.response.QRUrlContext;
import com.servemenu.businessservice.common.exception.ResourceNotFoundException;
import com.servemenu.businessservice.domain.enums.WifiType;
import com.servemenu.businessservice.domain.model.Business;
import com.servemenu.businessservice.domain.model.Store;
import com.servemenu.businessservice.domain.model.Table;
import com.servemenu.businessservice.domain.model.WifiSettings;
import com.servemenu.businessservice.domain.repository.StoreRepository;
import com.servemenu.businessservice.domain.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QRUrlService {
    
    @Value("${app.customer.base-url:https://menu.servemenu.com}")
    private String customerAppBaseUrl;

    
    /**
     * Generate QR URL from Table entity (NO database query)
     * Use this when you already have the Table object
     */
    public String generateQRUrl(Table table) {
        Store store = table.getStore();
        Business business = store.getBusiness();
        
        // NEW SHORT FORMAT: {businessSlug}/{storeCode}/{tableNumber}
        if (store.getStoreCode() != null && table.getTableNumber() != null) {
            return buildShortFormatUrl(business, store.getStoreCode(), table.getTableNumber());
        }
        
        // FALLBACK: Old UUID format (backward compatibility)
        return buildLongFormatUrl(business, store.getId(), table.getId());
    }
    
    /**
     * Build short format URL: {businessSlug}/{storeCode}/{tableNumber}
     * Example: https://menu.servemenu.com/piramidacafe-47c960/a1b2/T001
     */
    private String buildShortFormatUrl(Business business, String storeCode, String tableNumber) {
        String baseUrl = getBaseUrl(business);
        
        if (business.getCustomDomain() != null && business.getCustomDomainVerified()) {
            // Custom domain: https://restaurant.com/a1b2/T001
            return String.format("%s/%s/%s", baseUrl, storeCode, tableNumber);
        } else {
            // Default domain: https://menu.servemenu.com/piramidacafe-47c960/a1b2/T001
            return String.format("%s/%s/%s/%s", baseUrl, business.getSlug(), storeCode, tableNumber);
        }
    }
    
    /**
     * Build long format URL (fallback): store/{storeId}/table/{tableId}
     * Used when storeCode or tableNumber is missing
     */
    private String buildLongFormatUrl(Business business, UUID storeId, UUID tableId) {
        String baseUrl = getBaseUrl(business);
        
        if (business.getCustomDomain() != null && business.getCustomDomainVerified()) {
            // Custom domain: https://restaurant.com/menu/store/{storeId}/table/{tableId}
            return String.format("%s/menu/store/%s/table/%s", baseUrl, storeId, tableId);
        } else {
            // Default domain: https://menu.servemenu.com/{slug}/store/{storeId}/table/{tableId}
            return String.format("%s/%s/store/%s/table/%s", baseUrl, business.getSlug(), storeId, tableId);
        }
    }
    
    /**
     * Generate Business QR URL
     * Format: servemenu.com/{businessSlug} or customdomain.com
     */
    public String generateBusinessQRUrl(Business business) {
        String baseUrl = getBaseUrl(business);
        
        if (business.getCustomDomain() != null && business.getCustomDomainVerified()) {
            // Custom domain: https://restaurant.com
            return baseUrl;
        } else {
            // Default domain: https://menu.servemenu.com/piramidacafe-47c960
            return String.format("%s/%s", baseUrl, business.getSlug());
        }
    }
    
    /**
     * Generate WiFi QR string (not a URL, but WiFi configuration string)
     * Format: WIFI:T:{type};S:{ssid};P:{password};;
     */
    public String generateWifiQRString(WifiSettings wifiSettings) {
        String wifiType = mapWifiType(wifiSettings.getWifiType());
        String ssid = wifiSettings.getSsid();
        String password = wifiSettings.getPassword() != null ? wifiSettings.getPassword() : "";
        
        // WiFi QR format: WIFI:T:WPA;S:MyNetwork;P:MyPassword;;
        return String.format("WIFI:T:%s;S:%s;P:%s;;", wifiType, ssid, password);
    }
    
    /**
     * Map WifiType enum to WiFi QR string format
     */
    private String mapWifiType(WifiType wifiType) {
        return switch (wifiType) {
            case WPA -> "WPA";
            case WEB -> "WPA"; // Web auth also uses WPA
            case NO_ENCRYPTION -> "nopass";
        };
    }
    
    /**
     * Get base URL based on business configuration
     */
    private String getBaseUrl(Business business) {
        if (business.getCustomDomain() != null && business.getCustomDomainVerified()) {
            return "https://" + business.getCustomDomain();
        }
        return customerAppBaseUrl;
    }

}
