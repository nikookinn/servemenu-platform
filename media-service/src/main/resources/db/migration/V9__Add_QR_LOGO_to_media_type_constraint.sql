-- Add QR_LOGO to media_type check constraint
-- This migration updates the chk_media_type constraint to include the new QR_LOGO type

-- Drop existing constraint
ALTER TABLE media_assets DROP CONSTRAINT IF EXISTS chk_media_type;

-- Recreate constraint with QR_LOGO included
ALTER TABLE media_assets
ADD CONSTRAINT chk_media_type CHECK (
    media_type IN (
        'LOGO',
        'COVER_IMAGE', 
        'MENU_ITEM_IMAGE',
        'CATEGORY_IMAGE',
        'BUSINESS_QR',
        'TABLE_QR',
        'WIFI_QR',
        'QR_LOGO'
    )
);
