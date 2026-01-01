-- V2: Add multi-size S3 key columns for optimized image delivery
-- 
-- Size Strategy:
-- QR Codes (WiFi, Table): thumbnail=500x500, large=800x800
-- Menu Items: thumbnail=200x200, medium=400x400, large=800x800, original=1200x1200
-- Logo: thumbnail=200x200, large=1024x1024
-- Cover: thumbnail=400x225, large=1920x1080

-- Add thumbnail S3 key column
-- QR: 500x500, Items: 200x200, Logo: 200x200, Cover: 400x225
ALTER TABLE media_assets 
    ADD COLUMN thumbnail_s3_key VARCHAR(500);

-- Add medium S3 key column (only for MENU_ITEM type)
-- Items: 400x400
ALTER TABLE media_assets 
    ADD COLUMN medium_s3_key VARCHAR(500);

-- Add large S3 key column
-- QR: 800x800, Items: 800x800, Logo: 1024x1024, Cover: 1920x1080
ALTER TABLE media_assets 
    ADD COLUMN large_s3_key VARCHAR(500);

-- Create indexes for performance
CREATE INDEX idx_media_assets_thumbnail_s3_key ON media_assets(thumbnail_s3_key) WHERE thumbnail_s3_key IS NOT NULL;
CREATE INDEX idx_media_assets_medium_s3_key ON media_assets(medium_s3_key) WHERE medium_s3_key IS NOT NULL;
CREATE INDEX idx_media_assets_large_s3_key ON media_assets(large_s3_key) WHERE large_s3_key IS NOT NULL;

-- Add comments
COMMENT ON COLUMN media_assets.thumbnail_s3_key IS 'S3 key for thumbnail - QR:500x500, Items:200x200, Logo:200x200, Cover:400x225';
COMMENT ON COLUMN media_assets.medium_s3_key IS 'S3 key for medium size (400x400) - MENU_ITEM only';
COMMENT ON COLUMN media_assets.large_s3_key IS 'S3 key for large - QR:800x800, Items:800x800, Logo:1024x1024, Cover:1920x1080';

-- Migration Notes:
-- 1. Existing records will have NULL for new columns
-- 2. New uploads will generate sizes based on media type
-- 3. Bandwidth savings: ~99% for list views, ~97% for detail views
