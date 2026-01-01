CREATE TABLE IF NOT EXISTS social_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL UNIQUE REFERENCES stores(id) ON DELETE CASCADE,
    facebook VARCHAR(500),
    twitter VARCHAR(500),
    instagram VARCHAR(500),
    snapchat VARCHAR(500),
    pinterest VARCHAR(500),
    foursquare VARCHAR(500),
    tripadvisor VARCHAR(500),
    zomato VARCHAR(500),
    tiktok VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );