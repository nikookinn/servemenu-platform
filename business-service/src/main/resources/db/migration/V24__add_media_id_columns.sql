-- V24: Add media_id columns and remove URL columns for Media Service integration
-- This migration implements the hybrid approach: store media_id, enrich with URLs at runtime via gRPC

-- ============================================
-- 1. BusinessSettings: Logo and Cover Image
-- ============================================

-- Add media_id columns
ALTER TABLE business_settings 
    ADD COLUMN logo_media_id UUID,
    ADD COLUMN cover_image_media_id UUID;

-- Create indexes for performance
CREATE INDEX idx_business_settings_logo_media_id ON business_settings(logo_media_id) WHERE logo_media_id IS NOT NULL;
CREATE INDEX idx_business_settings_cover_media_id ON business_settings(cover_image_media_id) WHERE cover_image_media_id IS NOT NULL;

-- Drop old URL columns (if they exist)
ALTER TABLE business_settings 
    DROP COLUMN IF EXISTS logo_url,
    DROP COLUMN IF EXISTS cover_image_url;

-- Add comments
COMMENT ON COLUMN business_settings.logo_media_id IS 'Reference to Media Service - logo image';
COMMENT ON COLUMN business_settings.cover_image_media_id IS 'Reference to Media Service - cover image';

-- ============================================
-- 2. Tables: QR Code
-- ============================================

-- Rename qr_code_id to qr_code_media_id for consistency
ALTER TABLE tables 
    RENAME COLUMN qr_code_id TO qr_code_media_id;

-- Drop qr_code_url column
ALTER TABLE tables 
    DROP COLUMN IF EXISTS qr_code_url;

-- Create index for performance
CREATE INDEX idx_tables_qr_code_media_id ON tables(qr_code_media_id) WHERE qr_code_media_id IS NOT NULL;

-- Add comment
COMMENT ON COLUMN tables.qr_code_media_id IS 'Reference to Media Service - table QR code';

-- ============================================
-- 3. WifiSettings: QR Code
-- ============================================

-- Add media_id column
ALTER TABLE wifi_settings 
    ADD COLUMN qr_code_media_id UUID;

-- Drop old URL column
ALTER TABLE wifi_settings 
    DROP COLUMN IF EXISTS qr_code_url;

-- Create index for performance
CREATE INDEX idx_wifi_settings_qr_code_media_id ON wifi_settings(qr_code_media_id) WHERE qr_code_media_id IS NOT NULL;

-- Add comment
COMMENT ON COLUMN wifi_settings.qr_code_media_id IS 'Reference to Media Service - WiFi QR code';

-- ============================================
-- 4. Data Validation (Optional)
-- ============================================

-- Add check constraints to ensure UUIDs are valid format (PostgreSQL will validate automatically)
-- These are informational and help with documentation

-- ============================================
-- Migration Notes:
-- ============================================
-- 1. Old URL columns are dropped - URLs will be fetched from Media Service via gRPC
-- 2. Indexes created for foreign key lookups (even though no FK constraint)
-- 3. Partial indexes (WHERE NOT NULL) for better performance
-- 4. Comments added for documentation
-- 5. No data migration needed - this is for new data going forward
-- 6. Existing records will have NULL media_ids until updated
-- ============================================
