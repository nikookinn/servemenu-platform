package com.servemenu.businessservice.common.util;

import org.springframework.stereotype.Component;
import java.text.Normalizer;
import java.util.UUID;

// ========== SlugGenerator.java ==========
@Component
public class SlugGenerator {

    /**
     * Generate unique slug from business name
     * Example: "Starbucks Coffee" -> "starbucks-coffee-a1b2c3"
     */
    public String generate(String name, UUID ownerId) {
        // Normalize and clean the name
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase()
                .trim();

        // Replace spaces and special characters with hyphens
        String slug = normalized
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        // Add unique suffix (first 6 chars of owner UUID)
        String suffix = ownerId.toString().substring(0, 6);

        return slug + "-" + suffix;
    }

    /**
     * Validate slug format
     */
    public boolean isValid(String slug) {
        return slug != null &&
                slug.matches("^[a-z0-9]+(?:-[a-z0-9]+)*$") &&
                slug.length() >= 3 &&
                slug.length() <= 255;
    }
}
