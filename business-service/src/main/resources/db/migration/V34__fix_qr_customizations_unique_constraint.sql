-- Fix QR Customizations UNIQUE constraint
-- Problem: store_id is UNIQUE, but we need (store_id, qr_type) to be UNIQUE
-- This allows multiple QR types (TABLE, WIFI) per store

-- Step 1: Drop the old UNIQUE constraint on store_id only
ALTER TABLE qr_customizations 
    DROP CONSTRAINT IF EXISTS qr_customizations_store_id_key;

-- Step 2: Add new composite UNIQUE constraint for store-level QRs
-- This allows: store_id=X with qr_type=TABLE AND store_id=X with qr_type=WIFI
ALTER TABLE qr_customizations 
    ADD CONSTRAINT uq_qr_customization_store_type 
    UNIQUE (store_id, qr_type);

-- Step 3: Add composite UNIQUE constraint for business-level QRs
-- This allows: business_id=Y with qr_type=BUSINESS
ALTER TABLE qr_customizations 
    ADD CONSTRAINT uq_qr_customization_business_type 
    UNIQUE (business_id, qr_type);
