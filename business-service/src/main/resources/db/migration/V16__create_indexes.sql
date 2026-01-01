CREATE INDEX IF NOT EXISTS idx_stores_menu_id ON stores(menu_id) WHERE menu_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_tables_active ON tables(store_id, is_active);
CREATE INDEX IF NOT EXISTS idx_businesses_subscription ON businesses(subscription_plan, status);