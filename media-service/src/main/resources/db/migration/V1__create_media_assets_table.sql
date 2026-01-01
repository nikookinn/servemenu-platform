-- Media Assets Table
-- Stores metadata for all uploaded media (images, QR codes, etc.)
-- Uses soft delete pattern with deleted_at timestamp

CREATE TABLE IF NOT EXISTS media_assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- File Information
    original_filename VARCHAR(255) NOT NULL,
    s3_key VARCHAR(500) NOT NULL UNIQUE,
    s3_url VARCHAR(1000) NOT NULL,
    
    -- Media Classification
    media_type VARCHAR(50) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    
    -- File Metadata
    file_size BIGINT NOT NULL CHECK (file_size > 0),
    width INTEGER CHECK (width > 0),
    height INTEGER CHECK (height > 0),
    
    -- Relationships
    entity_id UUID NOT NULL,
    uploaded_by UUID,
    
    -- Audit Fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    
    -- Constraints
    CONSTRAINT chk_media_type CHECK (media_type IN ('LOGO', 'COVER_IMAGE', 'MENU_ITEM', 'TABLE_QR', 'WIFI_QR')),
    CONSTRAINT chk_content_type CHECK (content_type IN ('image/avif', 'image/webp', 'image/png', 'image/jpeg'))
);

-- Indexes for performance optimization
CREATE INDEX idx_media_entity_id ON media_assets(entity_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_media_type ON media_assets(media_type) WHERE deleted_at IS NULL;
CREATE INDEX idx_media_created_at ON media_assets(created_at DESC);
CREATE INDEX idx_media_deleted_at ON media_assets(deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX idx_media_uploaded_by ON media_assets(uploaded_by) WHERE deleted_at IS NULL;

-- Composite index for common queries
CREATE INDEX idx_media_entity_type ON media_assets(entity_id, media_type) WHERE deleted_at IS NULL;

-- Comments for documentation
COMMENT ON TABLE media_assets IS 'Stores metadata for all uploaded media assets including images and QR codes';
COMMENT ON COLUMN media_assets.s3_key IS 'Unique S3 object key in format: {type}/{entityId}/{timestamp}.{ext}';
COMMENT ON COLUMN media_assets.s3_url IS 'Full public URL to access the media asset';
COMMENT ON COLUMN media_assets.media_type IS 'Type of media: LOGO, COVER_IMAGE, MENU_ITEM, TABLE_QR, WIFI_QR';
COMMENT ON COLUMN media_assets.entity_id IS 'References the entity this media belongs to (business, menu item, table, etc.)';
COMMENT ON COLUMN media_assets.deleted_at IS 'Soft delete timestamp - NULL means active, non-NULL means deleted';
