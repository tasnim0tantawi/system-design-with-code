--liquibase formatted sql

--changeset auth-service:001 labels:initial comment:Create auth_user table
CREATE TABLE IF NOT EXISTS auth_user (
    id            BIGSERIAL    PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(255),
    role          VARCHAR(32)  NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);
--rollback DROP TABLE IF EXISTS auth_user;
