CREATE TABLE IF NOT EXISTS notification_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL UNIQUE REFERENCES businesses(id) ON DELETE CASCADE,
    order_notification_sound VARCHAR(100),
    order_notification_enabled BOOLEAN DEFAULT TRUE NOT NULL,
    feedback_notification_sound VARCHAR(100),
    feedback_notification_enabled BOOLEAN DEFAULT TRUE NOT NULL,
    hot_action_notification_sound VARCHAR(100),
    hot_action_notification_enabled BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );