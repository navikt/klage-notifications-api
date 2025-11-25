-- Create dead_letter_messages table for tracking failed Kafka messages
CREATE TABLE klage.dead_letter_messages
(
    id               UUID PRIMARY KEY,
    topic            TEXT      NOT NULL,
    message_key      TEXT,
    message_value    TEXT      NOT NULL,
    kafka_offset     BIGINT    NOT NULL,
    partition        INTEGER   NOT NULL,
    error_message    TEXT,
    stack_trace      TEXT,
    attempt_count    INTEGER   NOT NULL,
    first_attempt_at TIMESTAMP NOT NULL,
    last_attempt_at  TIMESTAMP NOT NULL,
    created_at       TIMESTAMP NOT NULL,
    processed_at     TIMESTAMP,
    processed        BOOLEAN   NOT NULL DEFAULT FALSE,
    reprocess        BOOLEAN   NOT NULL DEFAULT FALSE,
    reprocessed_at   TIMESTAMP
);
-- Create indexes for common queries
CREATE INDEX idx_dead_letter_messages_processed ON klage.dead_letter_messages (processed);
CREATE INDEX idx_dead_letter_messages_topic_processed ON klage.dead_letter_messages (topic, processed);
CREATE INDEX idx_dead_letter_messages_created_at ON klage.dead_letter_messages (created_at);
CREATE INDEX idx_dead_letter_messages_topic_offset_partition ON klage.dead_letter_messages (topic, kafka_offset, partition);
CREATE INDEX idx_dead_letter_messages_reprocess ON klage.dead_letter_messages (reprocess) WHERE reprocess = TRUE;