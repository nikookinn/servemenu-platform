-- ============================================================================
-- User Service - Core Tables Migration
-- Combines: users, business_owner_profiles, store_user_profiles, 
--           customer_profiles, user_preferences with all enhancements
-- ============================================================================

-- ============================================================================
-- 1. USERS TABLE
-- ============================================================================
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_user_id UUID NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    email_verified BOOLEAN DEFAULT FALSE,
    user_type VARCHAR(50) NOT NULL,
    account_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    version INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE,
    
    -- Constraints
    CONSTRAINT chk_user_type CHECK (user_type IN ('BUSINESS_OWNER', 'STORE_USER', 'CUSTOMER')),
    CONSTRAINT chk_account_status CHECK (account_status IN ('ACTIVE', 'SUSPENDED', 'DELETED')),
    CONSTRAINT chk_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT chk_first_name_not_empty CHECK (first_name IS NULL OR LENGTH(TRIM(first_name)) > 0),
    CONSTRAINT chk_last_name_not_empty CHECK (last_name IS NULL OR LENGTH(TRIM(last_name)) > 0)
);

-- Indexes
CREATE INDEX idx_users_keycloak_id ON users(keycloak_user_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_type ON users(user_type);
CREATE INDEX idx_users_status ON users(account_status);
CREATE INDEX idx_users_created_at ON users(created_at);

-- Comments
COMMENT ON TABLE users IS 'Base user table for all user types';
COMMENT ON COLUMN users.user_type IS 'Type of user: BUSINESS_OWNER, STORE_USER, or CUSTOMER';
COMMENT ON COLUMN users.account_status IS 'Current status of the account';
COMMENT ON COLUMN users.deleted_at IS 'Soft delete timestamp';

-- ============================================================================
-- 2. BUSINESS OWNER PROFILES TABLE
-- ============================================================================
CREATE TABLE business_owner_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    business_id UUID,
    onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE,
    subscription_plan VARCHAR(50) NOT NULL DEFAULT 'FREE',
    subscription_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    trial_ends_at TIMESTAMP WITH TIME ZONE,
    subscription_starts_at TIMESTAMP WITH TIME ZONE,
    subscription_ends_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    -- Foreign Keys
    CONSTRAINT fk_business_owner_profile_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT chk_subscription_plan CHECK (subscription_plan IN ('FREE', 'REGULAR', 'PREMIUM')),
    CONSTRAINT chk_subscription_status CHECK (subscription_status IN ('ACTIVE', 'TRIAL', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT chk_subscription_dates CHECK (
        subscription_ends_at IS NULL OR
        subscription_starts_at IS NULL OR
        subscription_ends_at > subscription_starts_at
    ),
    CONSTRAINT chk_trial_end_future CHECK (trial_ends_at IS NULL OR trial_ends_at > created_at)
);

-- Indexes
CREATE INDEX idx_business_owner_profiles_user_id ON business_owner_profiles(user_id);
CREATE INDEX idx_business_owner_profiles_business_id ON business_owner_profiles(business_id);
CREATE INDEX idx_business_owner_profiles_onboarding ON business_owner_profiles(onboarding_completed);
CREATE INDEX idx_business_owner_profiles_subscription ON business_owner_profiles(subscription_plan, subscription_status);

-- Comments
COMMENT ON TABLE business_owner_profiles IS 'Business owner specific profile data';
COMMENT ON COLUMN business_owner_profiles.business_id IS 'Business ID from business-service. Set when onboarding is completed via BusinessDetailsCompletedEvent';
COMMENT ON COLUMN business_owner_profiles.onboarding_completed IS 'Whether the business owner has completed the onboarding process';
COMMENT ON COLUMN business_owner_profiles.subscription_plan IS 'Current subscription plan';
COMMENT ON COLUMN business_owner_profiles.trial_ends_at IS 'Trial period end date';

-- ============================================================================
-- 3. STORE USER PROFILES TABLE
-- ============================================================================
CREATE TABLE store_user_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    business_id UUID NOT NULL,
    store_id UUID NOT NULL,
    access_level VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    -- Foreign Keys
    CONSTRAINT fk_store_user_profile_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT chk_store_user_access_level CHECK (access_level IN ('STORE_ADMIN', 'STORE_MANAGER', 'STORE_STAFF'))
);

-- Indexes
CREATE INDEX idx_store_user_profiles_user_id ON store_user_profiles(user_id);
CREATE INDEX idx_store_user_profiles_business_id ON store_user_profiles(business_id);
CREATE INDEX idx_store_user_profiles_store_id ON store_user_profiles(store_id);
CREATE INDEX idx_store_user_profiles_business_store ON store_user_profiles(business_id, store_id);

-- Comments
COMMENT ON TABLE store_user_profiles IS 'Store employee profile data';
COMMENT ON COLUMN store_user_profiles.business_id IS 'Business that owns the store';
COMMENT ON COLUMN store_user_profiles.access_level IS 'Access level within the store';
COMMENT ON COLUMN store_user_profiles.is_active IS 'Whether the employee is currently active';
COMMENT ON COLUMN store_user_profiles.created_by_user_id IS 'User who created this store user profile';

-- ============================================================================
-- 4. CUSTOMER PROFILES TABLE
-- ============================================================================
CREATE TABLE customer_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    phone_number VARCHAR(20),
    country_code VARCHAR(5),
    preferred_language VARCHAR(5) NOT NULL DEFAULT 'en',
    created_by_business_id UUID,
    loyalty_points INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    -- Foreign Keys
    CONSTRAINT fk_customer_profile_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT chk_phone_number_format CHECK (phone_number IS NULL OR phone_number ~ '^\+?[0-9]{7,15}$'),
    CONSTRAINT chk_loyalty_points_non_negative CHECK (loyalty_points >= 0)
);

-- Indexes
CREATE INDEX idx_customer_profiles_user_id ON customer_profiles(user_id);
CREATE INDEX idx_customer_profiles_business_id ON customer_profiles(created_by_business_id);
CREATE INDEX idx_customer_profiles_phone ON customer_profiles(phone_number);

-- Comments
COMMENT ON TABLE customer_profiles IS 'Customer specific profile data';
COMMENT ON COLUMN customer_profiles.country_code IS 'Country code for phone number (e.g. +90)';
COMMENT ON COLUMN customer_profiles.created_by_business_id IS 'Business that created this customer profile';
COMMENT ON COLUMN customer_profiles.loyalty_points IS 'Accumulated loyalty points';

-- ============================================================================
-- 5. USER PREFERENCES TABLE
-- ============================================================================
CREATE TABLE user_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    dashboard_language VARCHAR(5) NOT NULL DEFAULT 'en',
    timezone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    theme VARCHAR(20) NOT NULL DEFAULT 'LIGHT',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    -- Foreign Keys
    CONSTRAINT fk_user_preferences_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT chk_theme CHECK (theme IN ('LIGHT', 'DARK'))
);

-- Indexes
CREATE INDEX idx_user_preferences_user_id ON user_preferences(user_id);

-- Comments
COMMENT ON TABLE user_preferences IS 'User preferences for UI customization';
COMMENT ON COLUMN user_preferences.dashboard_language IS 'Preferred language for dashboard UI';
COMMENT ON COLUMN user_preferences.timezone IS 'User preferred timezone for displaying dates/times';
COMMENT ON COLUMN user_preferences.theme IS 'Dashboard theme preference: LIGHT or DARK';

-- ============================================================================
-- 6. TRIGGERS FOR UPDATED_AT
-- ============================================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_business_owner_profiles_updated_at BEFORE UPDATE ON business_owner_profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_customer_profiles_updated_at BEFORE UPDATE ON customer_profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_preferences_updated_at BEFORE UPDATE ON user_preferences
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
