-- Migration: Update WiFi Settings to support multiple WiFi networks per store
-- Version: V33
-- Description: 
--   - Remove UNIQUE constraint from store_id (allow multiple WiFi per store)
--   - Add wifi_name column (e.g., "Guest WiFi", "Staff WiFi")
--   - Add is_active column for soft delete
--   - Add qr_code_media_id column (already exists in model but not in V7)
--   - Remove qr_code_url (replaced by qr_code_media_id)

-- Step 1: Drop UNIQUE constraint on store_id
ALTER TABLE wifi_settings 
DROP CONSTRAINT IF EXISTS wifi_settings_store_id_key;

-- Step 2: Add wifi_name column
ALTER TABLE wifi_settings 
ADD COLUMN IF NOT EXISTS wifi_name VARCHAR(100);

-- Step 3: Set default wifi_name for existing records
UPDATE wifi_settings 
SET wifi_name = 'Guest WiFi' 
WHERE wifi_name IS NULL;

-- Step 4: Make wifi_name NOT NULL after setting defaults
ALTER TABLE wifi_settings 
ALTER COLUMN wifi_name SET NOT NULL;

-- Step 5: Add is_active column
ALTER TABLE wifi_settings 
ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT true;

-- Step 6: Add qr_code_media_id column (if not exists)
ALTER TABLE wifi_settings 
ADD COLUMN IF NOT EXISTS qr_code_media_id UUID;

-- Step 7: Drop qr_code_url column (deprecated, use qr_code_media_id instead)
ALTER TABLE wifi_settings 
DROP COLUMN IF EXISTS qr_code_url;

-- Step 8: Create unique index for active WiFi networks with same name in same store
-- This prevents duplicate WiFi names within a store (only for active records)
CREATE UNIQUE INDEX IF NOT EXISTS uk_wifi_settings_store_name 
ON wifi_settings (store_id, wifi_name) 
WHERE is_active = true;

-- Step 9: Create index on qr_code_media_id for faster lookups
CREATE INDEX IF NOT EXISTS idx_wifi_settings_qr_code_media_id 
ON wifi_settings(qr_code_media_id);

-- Step 10: Add comments for documentation
COMMENT ON COLUMN wifi_settings.wifi_name IS 'Name of the WiFi network (e.g., Guest WiFi, Staff WiFi)';
COMMENT ON COLUMN wifi_settings.is_active IS 'Soft delete flag - false means WiFi is deleted';
COMMENT ON COLUMN wifi_settings.qr_code_media_id IS 'Media ID of the WiFi QR code from Media Service';
