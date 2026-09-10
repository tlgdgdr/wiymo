-- Phase 8: blocks + reports

CREATE TABLE user_blocks (
    id              UUID PRIMARY KEY,
    blocker_id      UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    blocked_user_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_user_blocks_not_self CHECK (blocker_id <> blocked_user_id),
    CONSTRAINT ux_user_blocks_pair UNIQUE (blocker_id, blocked_user_id)
);

CREATE INDEX ix_user_blocks_blocker_id ON user_blocks (blocker_id);
CREATE INDEX ix_user_blocks_blocked_user_id ON user_blocks (blocked_user_id);

CREATE TABLE reports (
    id               UUID PRIMARY KEY,
    reporter_id      UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    reported_user_id UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    reason           VARCHAR(30)   NOT NULL,
    description      VARCHAR(1000),
    status           VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_reports_not_self CHECK (reporter_id <> reported_user_id)
);

CREATE INDEX ix_reports_reported_user_id ON reports (reported_user_id);
CREATE INDEX ix_reports_status ON reports (status);
