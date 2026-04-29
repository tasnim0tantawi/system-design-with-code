--liquibase formatted sql

--changeset booking-service:seed-bookings-001 labels:seed comment:Seed sample bookings (CONFIRMED + PENDING + CANCELLED)
INSERT INTO booking (id, user_id, hotel_id, room_id, check_in, check_out, status, total_price, created_at) VALUES
    (1, 4, 1, 1, CURRENT_DATE + 7,  CURRENT_DATE + 10, 'CONFIRMED', 660.00,  NOW() - INTERVAL '5 days'),
    (2, 4, 5, 10, CURRENT_DATE + 30, CURRENT_DATE + 33, 'CONFIRMED', 870.00,  NOW() - INTERVAL '3 days'),
    (3, 5, 3, 7, CURRENT_DATE + 14, CURRENT_DATE + 18, 'CONFIRMED', 1040.00, NOW() - INTERVAL '2 days'),
    (4, 5, 6, 13, CURRENT_DATE + 60, CURRENT_DATE + 63, 'PENDING',   960.00,  NOW() - INTERVAL '1 day'),
    (5, 6, 2, 4, CURRENT_DATE + 21, CURRENT_DATE + 24, 'CANCELLED', 420.00,  NOW() - INTERVAL '7 days'),
    (6, 6, 4, 9, CURRENT_DATE + 5,  CURRENT_DATE + 8,  'CONFIRMED', 330.00,  NOW() - INTERVAL '6 hours'),
    (7, 7, 5, 12, CURRENT_DATE + 90, CURRENT_DATE + 95, 'PENDING',   4500.00, NOW() - INTERVAL '2 hours')
ON CONFLICT (id) DO NOTHING;
--rollback DELETE FROM booking WHERE id BETWEEN 1 AND 7;

--changeset booking-service:reset-seq-002 labels:seed comment:Reset booking id sequence
SELECT setval('booking_id_seq', GREATEST((SELECT MAX(id) FROM booking), 7));
--rollback SELECT 1;
