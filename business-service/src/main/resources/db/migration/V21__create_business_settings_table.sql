-- Create business_settings table
CREATE TABLE business_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL UNIQUE,
    logo_url VARCHAR(500),
    cover_image_url VARCHAR(500),
    address TEXT,
    email VARCHAR(255),
    phone_number VARCHAR(20),
    country_code VARCHAR(5),
    enable_default_food_image BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_restaurant_settings_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE
);

-- Create index for faster lookups
CREATE INDEX idx_business_settings_business_id ON business_settings(business_id);
