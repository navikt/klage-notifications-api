-- Add new fields to notifications table for MeldingNotification
ALTER TABLE klage.notifications ADD COLUMN actor_navn TEXT;
ALTER TABLE klage.notifications ADD COLUMN saksnummer TEXT;
ALTER TABLE klage.notifications ADD COLUMN ytelse_id TEXT;
ALTER TABLE klage.notifications ADD COLUMN melding_created TIMESTAMP;

ALTER TABLE klage.notifications RENAME COLUMN sender_nav_ident to actor_nav_ident;