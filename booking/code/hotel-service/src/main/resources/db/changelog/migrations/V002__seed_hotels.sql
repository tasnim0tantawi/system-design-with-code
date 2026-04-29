--liquibase formatted sql

--changeset hotel-service:seed-hotels-001 labels:seed comment:Seed 6 hotels across Europe and Asia
INSERT INTO hotel (id, manager_id, name, description, location, stars, type, status) VALUES
    (1, 1, 'Grand Roma Palace',     'Historic 5-star palace overlooking the Spanish Steps',           'Rome, Italy',         5, 'luxury',   'verified'),
    (2, 1, 'Trastevere Boutique',   'Charming boutique hotel in the heart of Trastevere',             'Rome, Italy',         4, 'boutique', 'verified'),
    (3, 2, 'Barcelona Beach Resort', 'Beachfront resort with rooftop pool and tapas bar',              'Barcelona, Spain',    4, 'resort',   'verified'),
    (4, 2, 'Madrid City Inn',       'Affordable downtown stay near Puerta del Sol',                   'Madrid, Spain',       3, 'budget',   'verified'),
    (5, 3, 'Tokyo Sakura Tower',    'Modern high-rise hotel with views of Tokyo Bay',                 'Tokyo, Japan',        5, 'luxury',   'verified'),
    (6, 3, 'Kyoto Ryokan Sora',     'Traditional ryokan with tatami rooms and onsen',                 'Kyoto, Japan',        4, 'ryokan',   'verified')
ON CONFLICT (id) DO NOTHING;
--rollback DELETE FROM hotel WHERE id BETWEEN 1 AND 6;

--changeset hotel-service:seed-rooms-002 labels:seed comment:Seed room types per hotel
INSERT INTO room (id, hotel_id, room_type, base_price, total_count) VALUES
    (1,  1, 'standard', 220.00, 30),
    (2,  1, 'deluxe',   380.00, 15),
    (3,  1, 'suite',    750.00,  5),
    (4,  2, 'standard', 140.00, 20),
    (5,  2, 'deluxe',   210.00,  8),
    (6,  3, 'standard', 180.00, 40),
    (7,  3, 'sea-view', 260.00, 25),
    (8,  4, 'single',    75.00, 25),
    (9,  4, 'double',   110.00, 30),
    (10, 5, 'standard', 290.00, 50),
    (11, 5, 'deluxe',   450.00, 20),
    (12, 5, 'suite',    900.00,  6),
    (13, 6, 'tatami',   320.00, 12),
    (14, 6, 'tatami-onsen', 480.00, 6)
ON CONFLICT (id) DO NOTHING;
--rollback DELETE FROM room WHERE id BETWEEN 1 AND 14;

--changeset hotel-service:reset-sequences-003 labels:seed comment:Reset id sequences past seeded rows
SELECT setval('hotel_id_seq', GREATEST((SELECT MAX(id) FROM hotel), 6));
SELECT setval('room_id_seq',  GREATEST((SELECT MAX(id) FROM room),  14));
--rollback SELECT 1;

--changeset hotel-service:seed-availability-004 labels:seed comment:Seed 365 days of availability rows per seeded room
INSERT INTO room_availability (room_id, date, available_count)
SELECT r.id, dt::date, r.total_count
FROM room r
CROSS JOIN generate_series(CURRENT_DATE, CURRENT_DATE + INTERVAL '364 days', INTERVAL '1 day') dt
WHERE r.id BETWEEN 1 AND 14
ON CONFLICT (room_id, date) DO NOTHING;
--rollback DELETE FROM room_availability WHERE room_id BETWEEN 1 AND 14;
