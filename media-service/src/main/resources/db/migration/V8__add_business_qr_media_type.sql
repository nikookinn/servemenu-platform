-- Add BUSINESS_QR to media_type constraint
-- This allows storing QR codes for business-level (not table-specific)

-- Drop the old constraint
ALTER TABLE media_assets DROP CONSTRAINT IF EXISTS chk_media_type;

-- Add the new constraint with BUSINESS_QR
ALTER TABLE media_assets ADD CONSTRAINT chk_media_type 
    CHECK (media_type IN ('LOGO', 'COVER_IMAGE', 'MENU_ITEM', 'TABLE_QR', 'WIFI_QR', 'BUSINESS_QR'));

-- Update comment
COMMENT ON COLUMN media_assets.media_type IS 'Type of media: LOGO, COVER_IMAGE, MENU_ITEM, TABLE_QR, WIFI_QR, BUSINESS_QR';
