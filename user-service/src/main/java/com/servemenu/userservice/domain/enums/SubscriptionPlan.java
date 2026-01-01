package com.servemenu.userservice.domain.enums;

/**
 * Subscription plans with limits and features
 * -1 indicates unlimited
 */
public enum SubscriptionPlan {
    FREE(
        "Free Plan", 
        0.0,
        new PlanLimits(1, 2, 5, 5, false, false, false)
    ),
    REGULAR(
        "Regular Plan", 
        17.0,
        new PlanLimits(5, 10, 20, 50, true, true, false)
    ),
    PREMIUM(
        "Premium Plan", 
        49.0,
        new PlanLimits(-1, -1, -1, -1, true, true, true)  // -1 = unlimited
    );

    private final String displayName;
    private final Double monthlyPrice;
    private final PlanLimits limits;

    SubscriptionPlan(String displayName, Double monthlyPrice, PlanLimits limits) {
        this.displayName = displayName;
        this.monthlyPrice = monthlyPrice;
        this.limits = limits;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Double getMonthlyPrice() {
        return monthlyPrice;
    }

    public PlanLimits getLimits() {
        return limits;
    }

    /**
     * Plan limits configuration
     */
    public static class PlanLimits {
        private final int maxStores;           // -1 = unlimited
        private final int maxMenus;            // -1 = unlimited
        private final int maxCategoriesPerMenu;
        private final int maxItemsPerCategory;
        private final boolean wifiQrEnabled;
        private final boolean kitchenDisplayEnabled;
        private final boolean customDomainEnabled;

        public PlanLimits(
                int maxStores,
                int maxMenus,
                int maxCategoriesPerMenu,
                int maxItemsPerCategory,
                boolean wifiQrEnabled,
                boolean kitchenDisplayEnabled,
                boolean customDomainEnabled
        ) {
            this.maxStores = maxStores;
            this.maxMenus = maxMenus;
            this.maxCategoriesPerMenu = maxCategoriesPerMenu;
            this.maxItemsPerCategory = maxItemsPerCategory;
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