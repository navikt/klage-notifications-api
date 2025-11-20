CREATE TABLE klage.system_notifications
(
    id                UUID      NOT NULL PRIMARY KEY,
    title             TEXT      NOT NULL,
    message           TEXT      NOT NULL,
    source            TEXT      NOT NULL,
    created_at        TIMESTAMP NOT NULL,
    updated_at        TIMESTAMP NOT NULL,
    marked_as_deleted BOOLEAN   NOT NULL DEFAULT FALSE
);

-- Create indexes
CREATE INDEX idx_system_notifications_created_at
    ON klage.system_notifications (created_at);

CREATE INDEX idx_system_notifications_marked_as_deleted
    ON klage.system_notifications (marked_as_deleted);

-- Create system_notification_read_status table to track which users have read system notifications
CREATE TABLE klage.system_notification_read_status
(
    id                     UUID      NOT NULL PRIMARY KEY,
    system_notification_id UUID      NOT NULL,
    nav_ident              TEXT      NOT NULL,
    read_at                TIMESTAMP NOT NULL,
    CONSTRAINT fk_system_notification
        FOREIGN KEY (system_notification_id)
            REFERENCES klage.system_notifications (id)
            ON DELETE CASCADE
);

-- Create unique index to prevent duplicate read status entries
CREATE UNIQUE INDEX idx_system_notification_read_status_unique
    ON klage.system_notification_read_status (system_notification_id, nav_ident);

-- Create index on nav_ident
CREATE INDEX idx_system_notification_read_status_nav_ident
    ON klage.system_notification_read_status (nav_ident);
