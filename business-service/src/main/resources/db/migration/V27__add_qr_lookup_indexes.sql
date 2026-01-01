-- Composite indexes for optimized QR URL lookup
-- These indexes support the JOIN query in findByBusinessSlugAndStoreCodeAndTableNumber
-- Performance-critical for QR code scanning

-- Index for business slug lookup
CREATE INDEX IF NOT EXISTS idx_business_slug_lookup ON businesses(slug) 
INCLUDE (id);

-- Composite index for store lookup (business_id + store_code)
-- Already exists from V26, but ensure it's optimized
CREATE INDEX IF NOT EXISTS idx_store_qr_lookup ON stores(business_id, store_code) 
INCLUDE (id);

-- Composite index for table lookup (store_id + table_number)
CREATE INDEX IF NOT EXISTS idx_table_qr_lookup ON tables(store_id, table_number) 
INCLUDE (id);

-- Covering index for full QR path lookup (optional, for extreme performance)
-- This allows index-only scan without touching the table
CREATE INDEX IF NOT EXISTS idx_qr_full_path ON tables(table_number, store_id)
INCLUDE (id, table_name, is_active);

COMMENT ON INDEX idx_business_slug_lookup IS 'Optimizes business lookup in QR URL parsing';
COMMENT ON INDEX idx_store_qr_lookup IS 'Optimizes store lookup in QR URL parsing';
COMMENT ON INDEX idx_table_qr_lookup IS 'Optimizes table lookup in QR URL parsing - performance critical';
