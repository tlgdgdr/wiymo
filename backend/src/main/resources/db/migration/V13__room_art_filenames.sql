-- Room art files are named after the room they belong to
-- (cafe.jpg -> midnight_cafe.jpg), so the asset tree stays readable as more
-- scenes land: coffee_break.jpg, singles_lounge.jpg, world_cafe.jpg, ...
UPDATE rooms
SET background_image_url = '/rooms/midnight_cafe.jpg'
WHERE id = 'a1000000-0000-0000-0000-000000000001';
