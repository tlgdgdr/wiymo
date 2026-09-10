-- Phase 6: gifts, wallets, gift transactions

CREATE TABLE gifts (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(100) NOT NULL,
    icon_url      VARCHAR(500) NOT NULL,
    animation_url VARCHAR(500),
    coin_price    INTEGER      NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    category      VARCHAR(20)  NOT NULL,
    sort_order    INTEGER      NOT NULL DEFAULT 0
);

CREATE TABLE wallets (
    user_id      UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    coin_balance BIGINT      NOT NULL DEFAULT 0 CHECK (coin_balance >= 0),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE gift_transactions (
    id          UUID PRIMARY KEY,
    sender_id   UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    receiver_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    gift_id     UUID        NOT NULL REFERENCES gifts (id),
    coin_amount INTEGER     NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_gift_transactions_sender_id ON gift_transactions (sender_id);
CREATE INDEX ix_gift_transactions_receiver_id ON gift_transactions (receiver_id);

-- Launch catalog. Icons are emoji-rendered placeholders; swap for real art.
INSERT INTO gifts (name, icon_url, coin_price, category, sort_order) VALUES
    ('Coffee',     'https://placehold.co/128x128/59402c/f4f1fa.png?text=%E2%98%95', 10,  'CLASSIC',  1),
    ('Rose',       'https://placehold.co/128x128/5c2140/f4f1fa.png?text=%F0%9F%8C%B9', 30,  'ROMANTIC', 2),
    ('Heart',      'https://placehold.co/128x128/5c2140/f4f1fa.png?text=%E2%9D%A4', 50,  'ROMANTIC', 3),
    ('Teddy Bear', 'https://placehold.co/128x128/3d2a63/f4f1fa.png?text=%F0%9F%A7%B8', 100, 'FUN',      4),
    ('Crown',      'https://placehold.co/128x128/8a6d1d/f4f1fa.png?text=%F0%9F%91%91', 500, 'LUXURY',   5);
