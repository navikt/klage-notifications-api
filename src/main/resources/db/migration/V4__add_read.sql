-- Add new read column
ALTER TABLE klage.notifications ADD COLUMN read BOOLEAN NOT NULL DEFAULT FALSE;

-- Drop old status column
ALTER TABLE klage.notifications DROP COLUMN IF EXISTS status;

-- Create new indexes
CREATE INDEX idx_notifications_read ON klage.notifications (read);
CREATE INDEX idx_notifications_user_read ON klage.notifications (nav_ident, read);