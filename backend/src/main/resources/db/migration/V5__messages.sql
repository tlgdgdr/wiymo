-- Phase 5: private messages

CREATE TABLE messages (
    id           UUID PRIMARY KEY,
    sender_id    UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    receiver_id  UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    content      VARCHAR(1000) NOT NULL,
    message_type VARCHAR(20)   NOT NULL DEFAULT 'TEXT',
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    read_at      TIMESTAMPTZ
);

CREATE INDEX ix_messages_sender_id ON messages (sender_id);
CREATE INDEX ix_messages_receiver_id ON messages (receiver_id);
CREATE INDEX ix_messages_created_at ON messages (created_at);
-- Conversation lookups scan both directions of a pair.
CREATE INDEX ix_messages_pair ON messages (sender_id, receiver_id, created_at DESC);
CREATE INDEX ix_messages_unread ON messages (receiver_id, sender_id) WHERE read_at IS NULL;
