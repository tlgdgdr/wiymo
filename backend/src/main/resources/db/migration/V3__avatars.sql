-- Phase 3: avatar assets + user avatars

CREATE TABLE avatar_assets (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category     VARCHAR(20)  NOT NULL,
    asset_key    VARCHAR(50)  NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    image_url    VARCHAR(500) NOT NULL,
    premium      BOOLEAN      NOT NULL DEFAULT FALSE,
    coin_price   INTEGER      NOT NULL DEFAULT 0,
    enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order   INTEGER      NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX ux_avatar_assets_asset_key ON avatar_assets (asset_key);
CREATE INDEX ix_avatar_assets_category ON avatar_assets (category);

CREATE TABLE avatars (
    user_id       UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    body_key      VARCHAR(50) NOT NULL,
    face_key      VARCHAR(50),
    eyes_key      VARCHAR(50),
    hair_key      VARCHAR(50),
    top_key       VARCHAR(50),
    bottom_key    VARCHAR(50),
    shoes_key     VARCHAR(50),
    accessory_key VARCHAR(50),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Baseline asset catalog. Image URLs are placeholders; swap for real
-- transparent PNGs (same canvas size per category) when the art is ready.
INSERT INTO avatar_assets (category, asset_key, display_name, image_url, premium, coin_price, sort_order) VALUES
    ('BODY',      'body_01',    'Light',        'https://placehold.co/512x512/f2d3b8/f2d3b8.png', FALSE, 0,   1),
    ('BODY',      'body_02',    'Tan',          'https://placehold.co/512x512/d9a066/d9a066.png', FALSE, 0,   2),
    ('BODY',      'body_03',    'Deep',         'https://placehold.co/512x512/8d5524/8d5524.png', FALSE, 0,   3),
    ('FACE',      'face_01',    'Soft Smile',   'https://placehold.co/512x512/00000000/png?text=face_01', FALSE, 0, 1),
    ('FACE',      'face_02',    'Grin',         'https://placehold.co/512x512/00000000/png?text=face_02', FALSE, 0, 2),
    ('FACE',      'face_03',    'Calm',         'https://placehold.co/512x512/00000000/png?text=face_03', FALSE, 0, 3),
    ('EYES',      'eyes_01',    'Round',        'https://placehold.co/512x512/00000000/png?text=eyes_01', FALSE, 0, 1),
    ('EYES',      'eyes_02',    'Sharp',        'https://placehold.co/512x512/00000000/png?text=eyes_02', FALSE, 0, 2),
    ('HAIR',      'hair_01',    'Short Dark',   'https://placehold.co/512x512/00000000/png?text=hair_01', FALSE, 0, 1),
    ('HAIR',      'hair_02',    'Long Wavy',    'https://placehold.co/512x512/00000000/png?text=hair_02', FALSE, 0, 2),
    ('HAIR',      'hair_03',    'Curly',        'https://placehold.co/512x512/00000000/png?text=hair_03', FALSE, 0, 3),
    ('HAIR',      'hair_04',    'Buzz',         'https://placehold.co/512x512/00000000/png?text=hair_04', FALSE, 0, 4),
    ('TOP',       'top_01',     'Tee',          'https://placehold.co/512x512/00000000/png?text=top_01', FALSE, 0, 1),
    ('TOP',       'top_02',     'Hoodie',       'https://placehold.co/512x512/00000000/png?text=top_02', FALSE, 0, 2),
    ('TOP',       'top_03',     'Shirt',        'https://placehold.co/512x512/00000000/png?text=top_03', FALSE, 0, 3),
    ('TOP',       'top_04',     'Jacket',       'https://placehold.co/512x512/00000000/png?text=top_04', TRUE, 200, 4),
    ('BOTTOM',    'bottom_01',  'Jeans',        'https://placehold.co/512x512/00000000/png?text=bottom_01', FALSE, 0, 1),
    ('BOTTOM',    'bottom_02',  'Shorts',       'https://placehold.co/512x512/00000000/png?text=bottom_02', FALSE, 0, 2),
    ('BOTTOM',    'bottom_03',  'Skirt',        'https://placehold.co/512x512/00000000/png?text=bottom_03', FALSE, 0, 3),
    ('SHOES',     'shoes_01',   'Sneakers',     'https://placehold.co/512x512/00000000/png?text=shoes_01', FALSE, 0, 1),
    ('SHOES',     'shoes_02',   'Boots',        'https://placehold.co/512x512/00000000/png?text=shoes_02', FALSE, 0, 2),
    ('ACCESSORY', 'glasses_01', 'Glasses',      'https://placehold.co/512x512/00000000/png?text=glasses_01', FALSE, 0, 1),
    ('ACCESSORY', 'cap_01',     'Cap',          'https://placehold.co/512x512/00000000/png?text=cap_01', FALSE, 0, 2),
    ('ACCESSORY', 'crown_01',   'Crown',        'https://placehold.co/512x512/00000000/png?text=crown_01', TRUE, 500, 3);
