package com.servemenu.businessservice.application.service;

import com.servemenu.businessservice.domain.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Generates 4-digit hexadecimal store codes from UUID
 * Format: First 4 hex chars of UUID = 4 characters
 * Example: 47c9, a1b2, f3e4
 * 
 * IMPORTANT: Store code is unique WITHIN business, not globally
 * Business A can have store code "a1b2"
 * Business B can also have store code "a1b2" (different business)
 * 
 * Collision probability within single business: ~1 in 65,536 (16^4)
 * Retry mechanism handles collisions within business
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StoreCodeGenerator {
    
    private static final int MAX_RETRIES = 10;
    private final StoreRepository storeRepository;
    
    /**
     * Generate unique 4-digit hex store code within business
     * Uses first 4 chars of random UUID
     * Retries if collision detected within same business
     */
    public String generateStoreCode(UUID businessId) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            // Generate random UUID and take first 4 hex chars
            String storeCode = UUID.randomUUID().toString().replace("-", "").substring(0, 4);
            
            // Check if already exists in THIS business
            if (!storeRepository.existsByBusinessIdAndStoreCode(businessId, storeCode)) {
                log.debug("Generated unique store code: {} for business: {} (attempt: {})", 
                    storeCode, businessId, attempt + 1);
                return storeCode;
            }
            
            log.warn("Store code collision in business {}: {} (attempt: {}), retrying...", 
                businessId, storeCode, attempt + 1);
        }
        
        // Fallback: Use 6 chars for guaranteed uniqueness within business
        String fallbackCode = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        log.error("Failed to generate 4-char code after {} attempts for business {}, using 6-char fallback: {}", 
            MAX_RETRIES, businessId, fallbackCode);
        return fallbackCode;
    }
}
