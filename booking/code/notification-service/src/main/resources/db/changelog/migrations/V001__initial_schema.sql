--liquibase formatted sql

--changeset notification-service:001 labels:initial comment:Create notification table
CREATE TABLE IF NOT EXISTS notification (
    id         BIGSERIAL   PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    type       VARCHAR(64),
    channel    VARCHAR(32),
    message    TEXT,
    status     VARCHAR(32) NOT NULL DEFAULT 'queued',
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    read_at    TIMESTAMP
);
--rollback DROP TABLE IF EXISTS notification;

--changeset notification-service:002 labels:initial comment:Create notification_user index
CREATE INDEX IF NOT EXISTS notification_user_idx ON notification (user_id, created_at DESC);
--rollback DROP INDEX IF EXISTS notification_user_idx;
