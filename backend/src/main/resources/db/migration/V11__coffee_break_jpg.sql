-- Optimized art: same image re-encoded as JPG (2.3MB PNG -> ~370KB),
-- faster room entry on mobile connections.
UPDATE rooms
SET background_image_url = '/rooms/cafe.jpg'
WHERE id = 'a1000000-0000-0000-0000-000000000002';
