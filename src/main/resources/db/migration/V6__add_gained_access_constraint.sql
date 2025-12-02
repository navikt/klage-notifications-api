-- Add unique constraint for behandling_id + nav_ident for GAINED_ACCESS notifications
-- A user should only have one GAINED_ACCESS notification per behandling
CREATE UNIQUE INDEX idx_notifications_gained_access_unique
    ON klage.notifications (behandling_id, nav_ident)
    WHERE (notification_type = 'GAINED_ACCESS' AND marked_as_deleted = FALSE);