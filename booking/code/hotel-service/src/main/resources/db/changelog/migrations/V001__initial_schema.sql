--liquibase formatted sql

--changeset hotel-service:001 labels:initial comment:Create hotel table
CREATE TABLE IF NOT EXISTS hotel (
    id          BIGSERIAL    PRIMARY KEY,
    manager_id  BIGINT       NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    location    VARCHAR(255),
    stars       INT,
    type        VARCHAR(64),
    status      VARCHAR(32)  NOT NULL DEFAULT 'pending_verification',
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
--rollback DROP TABLE IF EXISTS hotel;

--changeset hotel-service:002 labels:initial comment:Create hotel_location index
CREATE INDEX IF NOT EXISTS hotel_location_idx ON hotel (LOWER(location));
--rollback DROP INDEX IF EXISTS hotel_location_idx;

--changeset hotel-service:003 labels:initial comment:Create room table
CREATE TABLE IF NOT EXISTS room (
    id          BIGSERIAL     PRIMARY KEY,
    hotel_id    BIGINT        NOT NULL REFERENCES hotel(id),
    room_type   VARCHAR(32)   NOT NULL,
    base_price  NUMERIC(10,2) NOT NULL,
    total_count INT           NOT NULL
);
--rollback DROP TABLE IF EXISTS room;

--changeset hotel-service:004 labels:initial comment:Create room_hotel index
CREATE INDEX IF NOT EXISTS room_hotel_idx ON room (hotel_id);
--rollback DROP INDEX IF EXISTS room_hotel_idx;

--changeset hotel-service:005 labels:initial comment:Create room_availability table
CREATE TABLE IF NOT EXISTS room_availability (
    id              BIGSERIAL PRIMARY KEY,
    room_id         BIGINT    NOT NULL REFERENCES room(id),
    date            DATE      NOT NULL,
    available_count INT       NOT NULL,
    CONSTRAINT room_availability_unique UNIQUE (room_id, date)
);
--rollback DROP TABLE IF EXISTS room_availability;

--changeset hotel-service:006 labels:initial comment:Create room_availability composite index
CREATE INDEX IF NOT EXISTS room_avail_room_date_idx ON room_availability (room_id, date);
--rollback DROP INDEX IF EXISTS room_avail_room_date_idx;
