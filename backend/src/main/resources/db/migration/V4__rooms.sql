-- Phase 4: rooms, seat slots, presence

CREATE TABLE rooms (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                 VARCHAR(100) NOT NULL,
    description          VARCHAR(300),
    theme                VARCHAR(20)  NOT NULL,
    intention            VARCHAR(30),
    background_image_url VARCHAR(500) NOT NULL,
    max_users            INTEGER      NOT NULL,
    active               BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE room_slots (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id    UUID          NOT NULL REFERENCES rooms (id) ON DELETE CASCADE,
    slot_index INTEGER       NOT NULL,
    x_percent  NUMERIC(5, 2) NOT NULL,
    y_percent  NUMERIC(5, 2) NOT NULL,
    scale      NUMERIC(4, 2) NOT NULL DEFAULT 1.0,
    CONSTRAINT ux_room_slots_room_index UNIQUE (room_id, slot_index)
);

-- user_id as PK: a user can only be in one room at a time, by schema.
CREATE TABLE room_presence (
    user_id    UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    room_id    UUID        NOT NULL REFERENCES rooms (id) ON DELETE CASCADE,
    slot_index INTEGER     NOT NULL,
    joined_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ux_room_presence_room_slot UNIQUE (room_id, slot_index)
);

CREATE INDEX ix_room_presence_room_id ON room_presence (room_id);
CREATE INDEX ix_room_slots_room_id ON room_slots (room_id);

-- Seed rooms. Background art is placeholder; swap URLs when real art lands.
INSERT INTO rooms (id, name, description, theme, intention, background_image_url, max_users) VALUES
    ('a1000000-0000-0000-0000-000000000001', 'Midnight Cafe',   'Low lights, slow talks.',            'NIGHT',    'DEEP_TALK',         'https://placehold.co/1080x1920/1a1030/3d2a63.png?text=Midnight+Cafe',   12),
    ('a1000000-0000-0000-0000-000000000002', 'Coffee Break',    'Drop in, say hi, sip something.',    'CAFE',     'CASUAL_CHAT',       'https://placehold.co/1080x1920/2b1d14/59402c.png?text=Coffee+Break',    12),
    ('a1000000-0000-0000-0000-000000000003', 'Singles Lounge',  'Warm corners and a little spark.',   'LOUNGE',   'FLIRT',             'https://placehold.co/1080x1920/2a0f1e/5c2140.png?text=Singles+Lounge',  12),
    ('a1000000-0000-0000-0000-000000000004', 'World Cafe',      'New faces from everywhere.',         'CAFE',     'MEET_PEOPLE',       'https://placehold.co/1080x1920/12222b/24485c.png?text=World+Cafe',      12),
    ('a1000000-0000-0000-0000-000000000005', 'Language Corner', 'Practice, laugh at mistakes, learn.','LANGUAGE', 'LANGUAGE_EXCHANGE', 'https://placehold.co/1080x1920/13261a/2a5238.png?text=Language+Corner', 12);

-- Shared 12-seat layout: three loose rows; lower rows slightly larger for depth.
INSERT INTO room_slots (room_id, slot_index, x_percent, y_percent, scale)
SELECT r.id, s.slot_index, s.x_percent, s.y_percent, s.scale
FROM rooms r
CROSS JOIN (VALUES
    (0,  20.00, 32.00, 0.85),
    (1,  50.00, 30.00, 0.85),
    (2,  80.00, 33.00, 0.85),
    (3,  12.00, 48.00, 0.95),
    (4,  38.00, 50.00, 0.95),
    (5,  63.00, 49.00, 0.95),
    (6,  88.00, 51.00, 0.95),
    (7,  20.00, 68.00, 1.05),
    (8,  50.00, 70.00, 1.05),
    (9,  80.00, 69.00, 1.05),
    (10, 35.00, 85.00, 1.15),
    (11, 65.00, 86.00, 1.15)
) AS s(slot_index, x_percent, y_percent, scale);
