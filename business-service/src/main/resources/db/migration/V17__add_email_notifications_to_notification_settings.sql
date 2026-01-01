-- Add email notification fields to notification_settings
-- These allow business to receive order notifications via email

ALTER TABLE notification_settings
    ADD COLUMN allow_order_email_notifications BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN order_notification_emails TEXT;

COMMENT ON COLUMN notification_settings.allow_order_email_notifications 
    IS 'Enable/disable email notifications for new orders';
COMMENT ON COLUMN notification_settings.order_notification_emails 
    IS 'Comma-separated email addresses for order notifications';

-- Create index for quick lookups
CREATE INDEX idx_notification_settings_email_enabled 
    ON notification_settings(allow_order_email_notifications) 
    WHERE allow_order_email_notifications = TRUE;

