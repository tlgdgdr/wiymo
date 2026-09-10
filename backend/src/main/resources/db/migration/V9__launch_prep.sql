-- Launch prep: push tokens + admin flag

ALTER TABLE users
    ADD COLUMN expo_push_token VARCHAR(100),
    ADD COLUMN is_admin BOOLEAN NOT NULL DEFAULT FALSE;
