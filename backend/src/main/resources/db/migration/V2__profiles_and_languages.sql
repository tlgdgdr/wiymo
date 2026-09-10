-- Phase 2: current intention + user languages

ALTER TABLE users
    ADD COLUMN current_intention VARCHAR(30);

CREATE TABLE user_languages (
    id            UUID PRIMARY KEY,
    user_id       UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    language_code VARCHAR(10) NOT NULL,
    type          VARCHAR(20) NOT NULL,
    level         VARCHAR(10) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ux_user_languages_user_code_type UNIQUE (user_id, language_code, type)
);

CREATE INDEX ix_user_languages_user_id ON user_languages (user_id);
CREATE INDEX ix_user_languages_language_code ON user_languages (language_code);
