-- Create QR Customizations table
CREATE TABLE qr_customizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL UNIQUE,
    
    -- QR Dimensions
    width INTEGER NOT NULL DEFAULT 800,
    height INTEGER NOT NULL DEFAULT 800,
    margin INTEGER NOT NULL DEFAULT 10,
    
    -- Colors
    background_color VARCHAR(7) NOT NULL DEFAULT '#FFFFFF',
    pattern_color_mode VARCHAR(20) NOT NULL DEFAULT 'single',
    pattern_color_single VARCHAR(7) DEFAULT '#000000',
    pattern_gradient_type VARCHAR(20),
    pattern_gradient_start VARCHAR(7),
    pattern_gradient_end VARCHAR(7),
    pattern_gradient_rotation INTEGER,
    
    -- Pattern & Eye Types
    pattern_type VARCHAR(20) NOT NULL DEFAULT 'square',
    eye_type VARCHAR(20) NOT NULL DEFAULT 'square',
    eye_color_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    eye_color_outer VARCHAR(7),
    eye_color_inner VARCHAR(7),
    
    -- Logo
    logo_media_id UUID,
    logo_size INTEGER DEFAULT 30,
    
    -- Frame
    frame_type VARCHAR(20) NOT NULL DEFAULT 'none',
    frame_text VARCHAR(100),
    frame_font VARCHAR(50) DEFAULT 'Arial',
    frame_color_mode VARCHAR(20) NOT NULL DEFAULT 'single',
    frame_color_single VARCHAR(7) DEFAULT '#000000',
    frame_gradient_type VARCHAR(20),
    frame_gradient_start VARCHAR(7),
    frame_gradient_end VARCHAR(7),
    frame_gradient_rotation INTEGER,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraint
    CONSTRAINT fk_qr_customization_store FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_qr_customization_store_id ON qr_customizations(store_id);

-- Add constraints for enum-like fields
ALTER TABLE qr_customizations ADD CONSTRAINT chk_pattern_color_mode 
    CHECK (pattern_color_mode IN ('single', 'gradient'));

ALTER TABLE qr_customizations ADD CONSTRAINT chk_pattern_gradient_type 
    CHECK (pattern_gradient_type IS NULL OR pattern_gradient_type IN ('linear', 'radial'));

ALTER TABLE qr_customizations ADD CONSTRAINT chk_pattern_type 
    CHECK (pattern_type IN ('square', 'rounded', 'dots', 'classy', 'classy-rounded', 'extra-rounded'));

ALTER TABLE qr_customizations ADD CONSTRAINT chk_eye_type 
    CHECK (eye_type IN ('square', 'dot', 'extra-rounded'));

ALTER TABLE qr_customizations ADD CONSTRAINT chk_frame_type 
    CHECK (frame_type IN ('none', 'circle-frame', 'bottom-text', 'top-text', 'box-frame'));

ALTER TABLE qr_customizations ADD CONSTRAINT chk_frame_color_mode 
    CHECK (frame_color_mode IN ('single', 'gradient'));

ALTER TABLE qr_customizations ADD CONSTRAINT chk_frame_gradient_type 
    CHECK (frame_gradient_type IS NULL OR frame_gradient_type IN ('linear', 'radial'));

-- Add constraints for color format (hex colors)
ALTER TABLE qr_customizations ADD CONSTRAINT chk_background_color_format 
    CHECK (background_color ~ '^#[0-9A-Fa-f]{6}$');

ALTER TABLE qr_customizations ADD CONSTRAINT chk_pattern_color_single_format 
    CHECK (pattern_color_single IS NULL OR pattern_color_single ~ '^#[0-9A-Fa-f]{6}$');

ALTER TABLE qr_customizations ADD CONSTRAINT chk_pattern_gradient_start_format 
    CHECK (pattern_gradient_start IS NULL OR pattern_gradient_start ~ '^#[0-9A-Fa-f]{6}$');

ALTER TABLE qr_customizations ADD CONSTRAINT chk_pattern_gradient_end_format 
    CHECK (pattern_gradient_end IS NULL OR pattern_gradient_end ~ '^#[0-9A-Fa-f]{6}$');

ALTER TABLE qr_customizations ADD CONSTRAINT chk_eye_color_outer_format 
    CHECK (eye_color_outer IS NULL OR eye_color_outer ~ '^#[0-9A-Fa-f]{6}$');

ALTER TABLE qr_customizations ADD CONSTRAINT chk_eye_color_inner_format 
    CHECK (eye_color_inner IS NULL OR eye_color_inner ~ '^#[0-9A-Fa-f]{6}$');

ALTER TABLE qr_customizations ADD CONSTRAINT chk_frame_color_single_format 
    CHECK (frame_color_single IS NULL OR frame_color_single ~ '^#[0-9A-Fa-f]{6}$');

ALTER TABLE qr_customizations ADD CONSTRAINT chk_frame_gradient_start_format 
    CHECK (frame_gradient_start IS NULL OR frame_gradient_start ~ '^#[0-9A-Fa-f]{6}$');

ALTER TABLE qr_customizations ADD CONSTRAINT chk_frame_gradient_end_format 
    CHECK (frame_gradient_end IS NULL OR frame_gradient_end ~ '^#[0-9A-Fa-f]{6}$');

-- Add constraints for numeric ranges
ALTER TABLE qr_customizations ADD CONSTRAINT chk_width_range 
    CHECK (width >= 100 AND width <= 2000);

ALTER TABLE qr_customizations ADD CONSTRAINT chk_height_range 
    CHECK (height >= 100 AND height <= 2000);

ALTER TABLE qr_customizations ADD CONSTRAINT chk_margin_range 
    CHECK (margin >= 0 AND margin <= 50);

ALTER TABLE qr_customizations ADD CONSTRAINT chk_logo_size_range 
    CHECK (logo_size IS NULL OR (logo_size >= 10 AND logo_size <= 50));

ALTER TABLE qr_customizations ADD CONSTRAINT chk_pattern_gradient_rotation_range 
    CHECK (pattern_gradient_rotation IS NULL OR (pattern_gradient_rotation >= 0 AND pattern_gradient_rotation <= 360));

ALTER TABLE qr_customizations ADD CONSTRAINT chk_frame_gradient_rotation_range 
    CHECK (frame_gradient_rotation IS NULL OR (frame_gradient_rotation >= 0 AND frame_gradient_rotation <= 360));

-- Add updated_at trigger
CREATE OR REPLACE FUNCTION update_qr_customizations_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_qr_customizations_updated_at
    BEFORE UPDATE ON qr_customizations
    FOR EACH ROW
    EXECUTE FUNCTION update_qr_customizations_updated_at();
