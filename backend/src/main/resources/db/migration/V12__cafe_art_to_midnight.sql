-- The café artwork reads as a slow, dusk-lit evening (candles, bookshelves,
-- plush seating) — that is DEEP_TALK energy, not the quick daytime drop-in
-- Coffee Break promises. Move the art (and its furniture-matched seating)
-- to Midnight Cafe; Coffee Break waits for brighter daytime art.

UPDATE rooms
SET background_image_url = '/rooms/cafe.jpg',
    max_users = 10
WHERE id = 'a1000000-0000-0000-0000-000000000001';

DELETE FROM room_slots WHERE room_id = 'a1000000-0000-0000-0000-000000000001';

INSERT INTO room_slots (room_id, slot_index, x_percent, y_percent, scale)
VALUES
    ('a1000000-0000-0000-0000-000000000001', 0, 18.00, 56.00, 0.90), -- sofa left
    ('a1000000-0000-0000-0000-000000000001', 1, 32.00, 55.00, 0.90), -- sofa right
    ('a1000000-0000-0000-0000-000000000001', 2, 38.00, 57.00, 0.90), -- mid table chair
    ('a1000000-0000-0000-0000-000000000001', 3, 50.00, 56.00, 0.90), -- mid table chair
    ('a1000000-0000-0000-0000-000000000001', 4, 63.00, 60.00, 0.95), -- bar stool
    ('a1000000-0000-0000-0000-000000000001', 5, 73.00, 60.00, 0.95), -- bar stool
    ('a1000000-0000-0000-0000-000000000001', 6, 82.00, 61.00, 0.95), -- bar stool
    ('a1000000-0000-0000-0000-000000000001', 7, 76.00, 72.00, 1.05), -- right table
    ('a1000000-0000-0000-0000-000000000001', 8, 22.00, 76.00, 1.10), -- green armchair
    ('a1000000-0000-0000-0000-000000000001', 9, 50.00, 82.00, 1.15); -- foreground

-- Coffee Break back to the placeholder and the shared 12-seat layout.
UPDATE rooms
SET background_image_url = 'https://placehold.co/1080x1920/2b1d14/59402c.png?text=Coffee+Break',
    max_users = 12
WHERE id = 'a1000000-0000-0000-0000-000000000002';

DELETE FROM room_slots WHERE room_id = 'a1000000-0000-0000-0000-000000000002';

INSERT INTO room_slots (room_id, slot_index, x_percent, y_percent, scale)
SELECT 'a1000000-0000-0000-0000-000000000002', s.slot_index, s.x_percent, s.y_percent, s.scale
FROM (VALUES
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

-- Anyone seated in a slot the re-layout removed loses their seat.
DELETE FROM room_presence rp
WHERE NOT EXISTS (
    SELECT 1 FROM room_slots s
    WHERE s.room_id = rp.room_id AND s.slot_index = rp.slot_index
);
