-- Phase 1: users + refresh tokens

CREATE TABLE users (
    id            UUID PRIMARY KEY,
    username      VARCHAR(30)  NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    birth_date    DATE         NOT NULL,
    country_code  VARCHAR(2),
    bio           VARCHAR(500),
    gender        VARCHAR(30),
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    online        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_seen_at  TIMESTAMPTZ
);

CREATE UNIQUE INDEX ux_users_username ON users (LOWER(username));
CREATE UNIQUE INDEX ux_users_email ON users (LOWER(email));

CREATE TABLE refresh_tokens (
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_refresh_tokens_token_hash ON refresh_tokens (token_hash);
CREATE INDEX ix_refresh_tokens_user_id ON refresh_tokens (user_id);
