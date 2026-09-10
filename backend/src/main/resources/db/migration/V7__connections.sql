-- Phase 7: connections

CREATE TABLE connections (
    id           UUID PRIMARY KEY,
    requester_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    addressee_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_connections_not_self CHECK (requester_id <> addressee_id)
);

CREATE INDEX ix_connections_requester_id ON connections (requester_id);
CREATE INDEX ix_connections_addressee_id ON connections (addressee_id);
-- One live (non-rejected) connection per unordered pair.
CREATE UNIQUE INDEX ux_connections_active_pair
    ON connections (LEAST(requester_id, addressee_id), GREATEST(requester_id, addressee_id))
    WHERE status IN ('PENDING', 'ACCEPTED', 'BLOCKED');
