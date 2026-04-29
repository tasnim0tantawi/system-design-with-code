--liquibase formatted sql

--changeset review-service:seed-reviews-001 labels:seed comment:Seed reviews tied to confirmed bookings
-- Each row maps to a CONFIRMED booking from booking-service's V002 seed:
--   booking 1: user 4 -> hotel 1
--   booking 2: user 4 -> hotel 5
--   booking 3: user 5 -> hotel 3
--   booking 6: user 6 -> hotel 4
-- The (user_id, booking_id) UNIQUE constraint prevents two reviews per booking.
INSERT INTO review (id, user_id, hotel_id, booking_id, rating, comment, created_at) VALUES
    (1, 4, 1, 1, 5, 'Stunning views and impeccable service. The breakfast was incredible.', NOW() - INTERVAL '4 days'),
    (2, 4, 5, 2, 4, 'Modern rooms, great location near Shibuya. Wifi was a bit slow.',      NOW() - INTERVAL '2 days'),
    (3, 5, 3, 3, 5, 'Loved the rooftop pool. Tapas bar staff were super friendly.',         NOW() - INTERVAL '1 day'),
    (4, 6, 4, 6, 3, 'Decent value for money but rooms felt cramped. Walls were thin.',      NOW() - INTERVAL '2 hours')
ON CONFLICT (id) DO NOTHING;
--rollback DELETE FROM review WHERE id BETWEEN 1 AND 4;

--changeset review-service:seed-aggregates-002 labels:seed comment:Seed review aggregates matching the seeded reviews
-- Aggregates must match the rows above EXACTLY:
--   hotel 1: 1 review (rating 5)
--   hotel 3: 1 review (rating 5)
--   hotel 4: 1 review (rating 3)
--   hotel 5: 1 review (rating 4)
INSERT INTO review_aggregate (hotel_id, total_reviews, sum_ratings, rating_1, rating_2, rating_3, rating_4, rating_5) VALUES
    (1, 1, 5, 0, 0, 0, 0, 1),
    (3, 1, 5, 0, 0, 0, 0, 1),
    (4, 1, 3, 0, 0, 1, 0, 0),
    (5, 1, 4, 0, 0, 0, 1, 0)
ON CONFLICT (hotel_id) DO UPDATE SET
    total_reviews = EXCLUDED.total_reviews,
    sum_ratings   = EXCLUDED.sum_ratings,
    rating_1 = EXCLUDED.rating_1,
    rating_2 = EXCLUDED.rating_2,
    rating_3 = EXCLUDED.rating_3,
    rating_4 = EXCLUDED.rating_4,
    rating_5 = EXCLUDED.rating_5;
--rollback DELETE FROM review_aggregate WHERE hotel_id IN (1, 3, 4, 5);

--changeset review-service:reset-seq-003 labels:seed comment:Reset review id sequence
SELECT setval('review_id_seq', GREATEST((SELECT MAX(id) FROM review), 4));
--rollback SELECT 1;
