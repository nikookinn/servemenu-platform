-- Add frame_text_color column to qr_customizations table
ALTER TABLE qr_customizations
ADD COLUMN frame_text_color VARCHAR(7) DEFAULT '#FFFFFF';

-- Update existing records to have white text color
UPDATE qr_customizations
SET frame_text_color = '#FFFFFF'
WHERE frame_text_color IS NULL;

-- Add comment
COMMENT ON COLUMN qr_customizations.frame_text_color IS 'Text color for frame text (hex format)';
