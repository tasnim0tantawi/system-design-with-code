--liquibase formatted sql

--changeset url-shortener:001 labels:initial comment:Create url_mapping table
CREATE TABLE IF NOT EXISTS url_mapping (
    short_code  VARCHAR(10) PRIMARY KEY,
    long_url    TEXT        NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMP   NOT NULL,
    creator_ip  VARCHAR(45)
);
--rollback DROP TABLE IF EXISTS url_mapping;

--changeset url-shortener:002 labels:initial comment:Index for periodic expiry sweep
CREATE INDEX IF NOT EXISTS url_mapping_expires_idx ON url_mapping (expires_at);
--rollback DROP INDEX IF EXISTS url_mapping_expires_idx;

--changeset url-shortener:003 labels:initial comment:Token range allocator counter
CREATE TABLE IF NOT EXISTS token_range (
    id          INT     PRIMARY KEY,
    next_value  BIGINT  NOT NULL
);
--rollback DROP TABLE IF EXISTS token_range;
