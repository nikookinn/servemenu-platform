-- ============================================================================
-- Migration: Add QR Type Support for Unified QR Architecture
-- Version: V32
-- Description: 
--   - Add qr_type column to qr_customizations table
--   - Add business_id column to qr_customizations (for BUSINESS QR)
--   - BUSINESS QR: business-level (business_id NOT NULL, store_id NULL)
--   - TABLE/WIFI QR: store-level (store_id NOT NULL, business_id NULL)
--   - Add business_qr_media_id to businesses table
--   - Add qr_customization_id to wifi_settings table
--   - Migrate existing data to TABLE type
-- ============================================================================

-- Step 1: Add qr_type column to qr_customizations
ALTER TABLE qr_customizations 
ADD COLUMN qr_type VARCHAR(20);

-- Step 2: Add business_id column to qr_customizations (for BUSINESS QR)
ALTER TABLE qr_customizations 
ADD COLUMN business_id UUID;

-- Step 3: Add foreign key constraint for business_id
ALTER TABLE qr_customizations 
ADD CONSTRAINT fk_qr_customization_business 
FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE;

-- Step 4: Set default value for existing records (all existing QR customizations are for tables)
UPDATE qr_customizations 
SET qr_type = 'TABLE' 
WHERE qr_type IS NULL;

-- Step 5: Make qr_type NOT NULL
ALTER TABLE qr_customizations 
ALTER COLUMN qr_type SET NOT NULL;

-- Step 6: Make store_id nullable (for BUSINESS QR)
ALTER TABLE qr_customizations 
ALTER COLUMN store_id DROP NOT NULL;

-- Step 7: Drop old unique constraint on store_id (if exists)
ALTER TABLE qr_customizations 
DROP CONSTRAINT IF EXISTS uk_qr_customization_store;

-- Step 8: Add unique constraint for store-level QR (TABLE, WIFI)
CREATE UNIQUE INDEX uk_qr_customization_store_type 
ON qr_customizations (store_id, qr_type) 
WHERE store_id IS NOT NULL;

-- Step 9: Add unique constraint for business-level QR (BUSINESS)
CREATE UNIQUE INDEX uk_qr_customization_business_type 
ON qr_customizations (business_id, qr_type) 
WHERE business_id IS NOT NULL;

-- Step 10: Add check constraint to ensure either business_id or store_id is set
ALTER TABLE qr_customizations 
ADD CONSTRAINT chk_qr_customization_owner 
CHECK (
    (business_id IS NOT NULL AND store_id IS NULL) OR 
    (business_id IS NULL AND store_id IS NOT NULL)
);

-- Step 6: Add business_qr_media_id to businesses table
ALTER TABLE businesses 
ADD COLUMN business_qr_media_id UUID;

-- Step 7: Add foreign key comment (optional, for documentation)
COMMENT ON COLUMN businesses.business_qr_media_id IS 'Media ID of the business QR code from Media Service';

-- Step 8: Add comment to wifi_settings.qr_code_media_id for clarity
-- Note: WiFi QR customization is found via store_id + qr_type='WIFI' in qr_customizations table
-- No direct foreign key needed - relationship is implicit through store_id
COMMENT ON COLUMN wifi_settings.qr_code_media_id IS 'Media ID of the WiFi QR code from Media Service';

-- Step 11: Create index on qr_type for faster queries
CREATE INDEX idx_qr_customizations_qr_type ON qr_customizations(qr_type);

-- Step 12: Create index on business_qr_media_id
CREATE INDEX idx_businesses_business_qr_media_id ON businesses(business_qr_media_id);

-- Step 13: Create index on business_id for faster queries
CREATE INDEX idx_qr_customizations_business_id ON qr_customizations(business_id);

-- ============================================================================
-- NOTE: QR Customization Initialization
-- ============================================================================
-- Default QR customizations are created by service layer:
-- - BUSINESS QR: Created in BusinessService.initializeDefaultSettings()
-- - TABLE QR: Created in StoreService.initializeDefaultStoreSettings()
-- - WIFI QR: Created in StoreService.initializeDefaultStoreSettings()
--
-- This ensures proper business logic and validation are applied.
-- ============================================================================

-- ============================================================================
-- Migration Complete
-- ============================================================================
