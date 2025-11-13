DO
$$
    BEGIN
        IF EXISTS
                (SELECT 1 FROM pg_roles WHERE rolname = 'cloudsqliamuser')
        THEN
            GRANT USAGE ON SCHEMA klage TO cloudsqliamuser;
            GRANT USAGE ON SCHEMA flyway_history_schema TO cloudsqliamuser;
            GRANT SELECT ON ALL TABLES IN SCHEMA klage TO cloudsqliamuser;
            ALTER DEFAULT PRIVILEGES IN SCHEMA flyway_history_schema GRANT SELECT ON TABLES TO cloudsqliamuser;
            ALTER DEFAULT PRIVILEGES IN SCHEMA klage GRANT SELECT ON TABLES TO cloudsqliamuser;
        END IF;
    END
$$;

CREATE TABLE klage.notifications
(
    id                 UUID PRIMARY KEY,
    notification_type  TEXT      NOT NULL, -- Discriminator column for inheritance
    title              TEXT      NOT NULL,
    message            TEXT      NOT NULL,
    nav_ident          TEXT      NOT NULL,
    severity           TEXT      NOT NULL,
    status             TEXT      NOT NULL,
    source             TEXT      NOT NULL,
    created_at         TIMESTAMP NOT NULL,
    updated_at         TIMESTAMP NOT NULL,
    read_at            TIMESTAMP,
    marked_as_deleted  BOOLEAN   NOT NULL DEFAULT FALSE,
    -- MeldingNotification and LostAccessNotification properties
    behandling_id      UUID,
    behandling_type_id TEXT,
    -- MeldingNotification specific properties
    melding_id         UUID,
    sender_nav_ident   TEXT
);

-- Create indexes for common queries
CREATE INDEX idx_notifications_user_id ON klage.notifications (nav_ident);
CREATE INDEX idx_notifications_status ON klage.notifications (status);
CREATE INDEX idx_notifications_created_at ON klage.notifications (created_at ASC);
CREATE INDEX idx_notifications_user_status ON klage.notifications (nav_ident, status);
CREATE INDEX idx_notifications_type ON klage.notifications (notification_type);
CREATE INDEX idx_notifications_behandling_id ON klage.notifications (behandling_id) WHERE behandling_id IS NOT NULL;
