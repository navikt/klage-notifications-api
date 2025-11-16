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
    id                 UUID                  NOT NULL PRIMARY KEY,
    notification_type  TEXT                  NOT NULL,
    message            TEXT                  NOT NULL,
    nav_ident          TEXT                  NOT NULL,
    source             TEXT                  NOT NULL,
    created_at         TIMESTAMP             NOT NULL,
    updated_at         TIMESTAMP             NOT NULL,
    read_at            TIMESTAMP,
    marked_as_deleted  BOOLEAN DEFAULT FALSE NOT NULL,
    behandling_id      UUID,
    behandling_type_id TEXT,
    melding_id         UUID,
    actor_nav_ident    TEXT,
    kafka_message_id   UUID,
    read               BOOLEAN DEFAULT FALSE NOT NULL,
    actor_navn         TEXT,
    saksnummer         TEXT,
    ytelse_id          TEXT,
    source_created_at  TIMESTAMP
);

CREATE INDEX idx_notifications_user_id
    ON klage.notifications (nav_ident);

CREATE INDEX idx_notifications_created_at
    ON klage.notifications (created_at);

CREATE INDEX idx_notifications_type
    ON klage.notifications (notification_type);

CREATE INDEX idx_notifications_behandling_id
    ON klage.notifications (behandling_id)
    WHERE (behandling_id IS NOT NULL);

CREATE UNIQUE INDEX idx_notifications_kafka_message_id
    ON klage.notifications (kafka_message_id)
    WHERE (kafka_message_id IS NOT NULL);

CREATE INDEX idx_notifications_read
    ON klage.notifications (read);

CREATE INDEX idx_notifications_user_read
    ON klage.notifications (nav_ident, read);
