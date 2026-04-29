--liquibase formatted sql

--changeset review-service:001 labels:initial comment:Create review table
CREATE TABLE IF NOT EXISTS review (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT    NOT NULL,
    hotel_id   BIGINT    NOT NULL,
    booking_id BIGINT    NOT NULL,
    rating     INT       NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment    TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT review_unique_user_booking UNIQUE (user_id, booking_id)
);
--rollback DROP TABLE IF EXISTS review;

--changeset review-service:002 labels:initial comment:Create review_hotel index
CREATE INDEX IF NOT EXISTS review_hotel_idx ON review (hotel_id, created_at DESC);
--rollback DROP INDEX IF EXISTS review_hotel_idx;

--changeset review-service:003 labels:initial comment:Create review_aggregate table
CREATE TABLE IF NOT EXISTS review_aggregate (
    hotel_id      BIGINT PRIMARY KEY,
    total_reviews INT    NOT NULL DEFAULT 0,
    sum_ratings   BIGINT NOT NULL DEFAULT 0,
    rating_1      INT    NOT NULL DEFAULT 0,
    rating_2      INT    NOT NULL DEFAULT 0,
    rating_3      INT    NOT NULL DEFAULT 0,
    rating_4      INT    NOT NULL DEFAULT 0,
    rating_5      INT    NOT NULL DEFAULT 0
);
--rollback DROP TABLE IF EXISTS review_aggregate;
