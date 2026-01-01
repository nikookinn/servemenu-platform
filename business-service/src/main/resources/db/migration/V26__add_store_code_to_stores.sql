-- Add store_code column to stores table for shorter QR URLs
-- Format: 4-digit hexadecimal from UUID (e.g., a1b2, 47c9, f3e4)
-- Example URL: https://menu.servemenu.com/piramidacafe-47c960/a1b2/T001
-- 
-- IMPORTANT: Store code is unique WITHIN business, not globally
-- Business A can have store code "a1b2"
-- Business B can also have store code "a1b2" (different business)
--
-- NOTE: Only new stores will have store_code
-- Existing stores will have NULL (they use old UUID-based QR URLs)

-- Add column as nullable (existing stores will remain NULL)
ALTER TABLE stores
ADD COLUMN store_code VARCHAR(6);

-- Create composite index for fast lookup (business_id + store_code)
CREATE INDEX idx_store_business_code ON stores(business_id, store_code)
WHERE store_code IS NOT NULL;

-- Add composite unique constraint (unique within business)
-- Only applies to stores with non-null store_code
ALTER TABLE stores
ADD CONSTRAINT uk_store_business_code UNIQUE (business_id, store_code);

COMMENT ON COLUMN stores.store_code IS '4-digit hex code from UUID, unique within business. NULL for old stores. Example: a1b2';
