--liquibase formatted sql

--changeset booking-service:001 labels:initial comment:Create booking table
CREATE TABLE IF NOT EXISTS booking (
    id          BIGSERIAL     PRIMARY KEY,
    user_id     BIGINT        NOT NULL,
    hotel_id    BIGINT        NOT NULL,
    room_id     BIGINT        NOT NULL,
    check_in    DATE          NOT NULL,
    check_out   DATE          NOT NULL,
    status      VARCHAR(32)   NOT NULL,
    total_price NUMERIC(10,2) NOT NULL,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);
--rollback DROP TABLE IF EXISTS booking;

--changeset booking-service:002 labels:initial comment:Create booking indexes
CREATE INDEX IF NOT EXISTS booking_user_idx   ON booking (user_id);
CREATE INDEX IF NOT EXISTS booking_hotel_idx  ON booking (hotel_id);
CREATE INDEX IF NOT EXISTS booking_status_idx ON booking (status);
--rollback DROP INDEX IF EXISTS booking_status_idx; DROP INDEX IF EXISTS booking_hotel_idx; DROP INDEX IF EXISTS booking_user_idx;
