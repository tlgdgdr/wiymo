-- Real art for Coffee Break: pixel-art café (static/rooms/cafe.png).
-- Relative URLs resolve against the API base on the client, so the same
-- row works in every environment.

UPDATE rooms
SET background_image_url = '/rooms/cafe.png'
WHERE id = 'a1000000-0000-0000-0000-000000000002';

-- Seats matched to the artwork's furniture: sofa, tables, bar stools,
-- armchair, foreground. 10 seats fit this composition (12 crowds it).
DELETE FROM room_slots WHERE room_id = 'a1000000-0000-0000-0000-000000000002';

INSERT INTO room_slots (room_id, slot_index, x_percent, y_percent, scale)
VALUES
    ('a1000000-0000-0000-0000-000000000002', 0, 18.00, 56.00, 0.90), -- sofa left
    ('a1000000-0000-0000-0000-000000000002', 1, 32.00, 55.00, 0.90), -- sofa right
    ('a1000000-0000-0000-0000-000000000002', 2, 38.00, 57.00, 0.90), -- mid table chair
    ('a1000000-0000-0000-0000-000000000002', 3, 50.00, 56.00, 0.90), -- mid table chair
    ('a1000000-0000-0000-0000-000000000002', 4, 63.00, 60.00, 0.95), -- bar stool
    ('a1000000-0000-0000-0000-000000000002', 5, 73.00, 60.00, 0.95), -- bar stool
    ('a1000000-0000-0000-0000-000000000002', 6, 82.00, 61.00, 0.95), -- bar stool
    ('a1000000-0000-0000-0000-000000000002', 7, 76.00, 72.00, 1.05), -- right table
    ('a1000000-0000-0000-0000-000000000002', 8, 22.00, 76.00, 1.10), -- green armchair
    ('a1000000-0000-0000-0000-000000000002', 9, 50.00, 82.00, 1.15); -- foreground

UPDATE rooms
SET max_users = 10
WHERE id = 'a1000000-0000-0000-0000-000000000002';
