-- Add unique constraint for melding_id to ensure idempotency for MELDING notifications
-- A meldingId should only exist once in the system
CREATE UNIQUE INDEX idx_notifications_melding_id_unique
    ON klage.notifications (melding_id)
    WHERE (melding_id IS NOT NULL AND notification_type = 'MELDING' AND marked_as_deleted = FALSE);

-- Add unique constraint for behandling_id + nav_ident for LOST_ACCESS notifications
-- A user should only have one LOST_ACCESS notification per behandling
CREATE UNIQUE INDEX idx_notifications_lost_access_unique
    ON klage.notifications (behandling_id, nav_ident)
    WHERE (notification_type = 'LOST_ACCESS' AND marked_as_deleted = FALSE);