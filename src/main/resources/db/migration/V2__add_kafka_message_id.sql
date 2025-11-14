ALTER TABLE klage.notifications
    ADD COLUMN kafka_message_id UUID;

CREATE UNIQUE INDEX idx_notifications_kafka_message_id ON klage.notifications (kafka_message_id) WHERE kafka_message_id IS NOT NULL;
