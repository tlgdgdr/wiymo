-- Language exchange is hidden for the MVP launch (kept in the schema so it can
-- be switched back on without a data migration), and replaced on the Home
-- screen by a new "play games" mood with its own room.

UPDATE rooms SET active = FALSE
WHERE id = 'a1000000-0000-0000-0000-000000000005';

INSERT INTO rooms (id, name, description, theme, intention, background_image_url, max_users) VALUES
    ('a1000000-0000-0000-0000-000000000006', 'Game Room', 'Pick a table, play a round.', 'GAME', 'PLAY_GAMES',
     'https://placehold.co/1080x1920/16121f/3a2f52.png?text=Game+Room', 8);

-- Four two-seat tables: players sit facing each other, so a 1v1 game reads
-- naturally from the seating alone.
INSERT INTO room_slots (room_id, slot_index, x_percent, y_percent, scale) VALUES
    ('a1000000-0000-0000-0000-000000000006', 0, 26.00, 38.00, 0.85),
    ('a1000000-0000-0000-0000-000000000006', 1, 44.00, 38.00, 0.85),
    ('a1000000-0000-0000-0000-000000000006', 2, 62.00, 40.00, 0.90),
    ('a1000000-0000-0000-0000-000000000006', 3, 80.00, 40.00, 0.90),
    ('a1000000-0000-0000-0000-000000000006', 4, 20.00, 64.00, 1.00),
    ('a1000000-0000-0000-0000-000000000006', 5, 38.00, 65.00, 1.00),
    ('a1000000-0000-0000-0000-000000000006', 6, 60.00, 80.00, 1.10),
    ('a1000000-0000-0000-0000-000000000006', 7, 80.00, 81.00, 1.10);

-- Nobody is left sitting in a room the app no longer lists.
DELETE FROM room_presence
WHERE room_id = 'a1000000-0000-0000-0000-000000000005';

-- Users whose mood was language exchange lose it rather than being stranded
-- pointing at a hidden room.
UPDATE users SET current_intention = NULL WHERE current_intention = 'LANGUAGE_EXCHANGE';

-- One table per game, whatever the game. `state` is engine-owned and opaque to
-- the rest of the app: tic-tac-toe stores a 9-character board, later games
-- (tombala, okey) store their own encoding without a schema change.
CREATE TABLE game_sessions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_type     VARCHAR(30)  NOT NULL,
    room_id       UUID         REFERENCES rooms (id) ON DELETE SET NULL,
    challenger_id UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    opponent_id   UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status        VARCHAR(20)  NOT NULL,
    turn_user_id  UUID         REFERENCES users (id) ON DELETE SET NULL,
    winner_id     UUID         REFERENCES users (id) ON DELETE SET NULL,
    state         VARCHAR(1000) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_game_sessions_distinct_players CHECK (challenger_id <> opponent_id)
);

-- Live games are looked up constantly (every move, every join); finished ones
-- never are, so the indexes only cover the live ones.
CREATE INDEX ix_game_sessions_challenger_live ON game_sessions (challenger_id)
    WHERE status IN ('PENDING', 'ACTIVE');
CREATE INDEX ix_game_sessions_opponent_live ON game_sessions (opponent_id)
    WHERE status IN ('PENDING', 'ACTIVE');
