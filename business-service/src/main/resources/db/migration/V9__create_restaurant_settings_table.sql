CREATE TABLE IF NOT EXISTS restaurant_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL UNIQUE REFERENCES businesses(id) ON DELETE CASCADE,
    logo_url VARCHAR(500),
    cover_image_url VARCHAR(500),
    address VARCHAR(500),
    email VARCHAR(255),
    phone_number VARCHAR(20),
    country_code VARCHAR(5),
    enable_default_food_image BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );