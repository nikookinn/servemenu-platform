-- Tax table already has percentage fields from V12 migration
-- This migration is a no-op but kept for consistency
-- Fields already present: dine_in_percentage, take_out_percentage

-- Add comments for clarity
COMMENT ON COLUMN taxes.dine_in_percentage IS 'Tax percentage for dine-in orders (0-100)';
COMMENT ON COLUMN taxes.take_out_percentage IS 'Tax percentage for takeout orders (0-100)';

-- Add constraints if not already present (will fail silently if exists)
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'chk_dine_in_percentage'
    ) THEN
        ALTER TABLE taxes 
            ADD CONSTRAINT chk_dine_in_percentage 
            CHECK (dine_in_percentage >= 0 AND dine_in_percentage <= 100);
    END IF;
    
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'chk_take_out_percentage'
    ) THEN
        ALTER TABLE taxes 
            ADD CONSTRAINT chk_take_out_percentage 
            CHECK (take_out_percentage >= 0 AND take_out_percentage <= 100);
    END IF;
END $$;

