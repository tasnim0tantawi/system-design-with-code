--liquibase formatted sql

--changeset notification-service:seed-notifications-001 labels:seed comment:Seed sample notifications for guest users
INSERT INTO notification (id, user_id, type, channel, message, status, created_at, read_at) VALUES
    (1, 4, 'booking_confirmed', 'in_app', 'Booking #1 at Grand Roma Palace confirmed.',          'delivered', NOW() - INTERVAL '5 days', NOW() - INTERVAL '4 days'),
    (2, 4, 'booking_confirmed', 'in_app', 'Booking #2 at Tokyo Sakura Tower confirmed.',         'delivered', NOW() - INTERVAL '3 days', NULL),
    (3, 5, 'booking_confirmed', 'in_app', 'Booking #3 at Barcelona Beach Resort confirmed.',     'delivered', NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day'),
    (4, 6, 'booking_cancelled', 'in_app', 'Booking #5 at Trastevere Boutique cancelled.',        'delivered', NOW() - INTERVAL '6 days', NULL),
    (5, 6, 'booking_confirmed', 'in_app', 'Booking #6 at Madrid City Inn confirmed.',            'delivered', NOW() - INTERVAL '6 hours', NULL),
    (6, 4, 'review_submitted',  'in_app', 'Thanks for reviewing Grand Roma Palace!',             'delivered', NOW() - INTERVAL '4 days', NOW() - INTERVAL '3 days'),
    (7, 7, 'booking_pending',   'in_app', 'Payment pending for booking #7 at Tokyo Sakura Tower.', 'queued',  NOW() - INTERVAL '2 hours', NULL)
ON CONFLICT (id) DO NOTHING;
--rollback DELETE FROM notification WHERE id BETWEEN 1 AND 7;

--changeset notification-service:reset-seq-002 labels:seed comment:Reset notification id sequence
SELECT setval('notification_id_seq', GREATEST((SELECT MAX(id) FROM notification), 7));
--rollback SELECT 1;
