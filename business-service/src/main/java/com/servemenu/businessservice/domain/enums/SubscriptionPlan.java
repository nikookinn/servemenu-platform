package com.servemenu.businessservice.domain.enums;

import lombok.Getter;

/**
 * Subscription plans with limits and features
 * -1 indicates unlimited
 */
@Getter
public enum SubscriptionPlan {
    FREE(
        "Free Plan",
        0.0,
        new PlanLimits(2, 2, 5, 5, 10, false, false, false)
    ),
    REGULAR(
        "Regular Plan",
        17.0,
        new PlanLimits(5, 10, 20, 50, 50, true, true, false)
    ),
    PREMIUM(
        "Premium Plan",
        49.0,
        new PlanLimits(-1, -1, -1, -1, -1, true, true, true)  // -1 = unlimited
    );

    private final String displayName;
    private final Double monthlyPrice;
    private final PlanLimits limits;

    SubscriptionPlan(String displayName, Double monthlyPrice, PlanLimits limits) {
        this.displayName = displayName;
        this.monthlyPrice = monthlyPrice;
        this.limits = limits;
    }

    // Legacy compatibility method
    public Integer getMaxStores() {
        return limits.getMaxStores();
    }

    public boolean allowsUnlimitedStores() {
        return limits.isUnlimitedStores();
    }

    /**
     * Plan limits configuration
     */
    public static class PlanLimits {
        private final int maxStores;           // -1 = unlimited
        private final int maxMenus;            // -1 = unlimited
        private final int maxCategoriesPerMenu;
        private final int maxItemsPerCategory;
        private final int maxTablesPerStore;   // -1 = unlimited
        private final boolean wifiQrEnabled;
        private final boolean kitchenDisplayEnabled;
        private final boolean customDomainEnabled;

        public PlanLimits(
                int maxStores,
                int maxMenus,
                int maxCategoriesPerMenu,
                int maxItemsPerCategory,
                int maxTablesPerStore,
                boolean wifiQrEnabled,
                boolean kitchenDisplayEnabled,
                boolean customDomainEnabled
        ) {
            this.maxStores = maxStores;
            this.maxMenus = maxMenus;
            this.maxCategoriesPerMenu = maxCategoriesPerMenu;
            this.maxItemsPerCategory = maxItemsPerCategory;
            this.maxTablesPerStore = maxTablesPerStore;
            this.wifiQrEnabled = wifiQrEnabled;
            this.kitchenDisplayEnabled = kitchenDisplayEnabled;
            this.customDomainEnabled = customDomainEnabled;
        }

        public int getMaxStores() {
            return maxStores;
        }

        public int getMaxMenus() {
            return maxMenus;
        }

        public int getMaxCategoriesPerMenu() {
            return maxCategoriesPerMenu;
        }

        public int getMaxItemsPerCategory() {
            return maxItemsPerCategory;
        }

        public int getMaxTablesPerStore() {
            return maxTablesPerStore;
        }

        public boolean isWifiQrEnabled() {
            return wifiQrEnabled;
        }

        public boolean isKitchenDisplayEnabled() {
            return kitchenDisplayEnabled;
        }

        public boolean isCustomDomainEnabled() {
            return customDomainEnabled;
        }

        public boolean isUnlimitedStores() {
            return maxStores == -1;
        }

        public boolean isUnlimitedMenus() {
            return maxMenus == -1;
        }
    }
}
