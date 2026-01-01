package com.servemenu.businessservice.domain.enums;

/**
 * QR Code Types
 * Each store can have 3 types of QR codes with different customizations
 */
public enum QRType {
    /**
     * Business QR - Points to business landing page
     * URL: servemenu.com/{businessSlug}
     * Created automatically when business is created
     */
    BUSINESS,
    
    /**
     * Table QR - Points to specific table menu
     * URL: servemenu.com/t/{tableSlug}
     * Created automatically when table is created
     */
    TABLE,
    
    /**
     * WiFi QR - Contains WiFi credentials
     * Format: WIFI:T:WPA;S:ssid;P:password;;
     * Created manually by user in WiFi settings
     */
    WIFI
}
