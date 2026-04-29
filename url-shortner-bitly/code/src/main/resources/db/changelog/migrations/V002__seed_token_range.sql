--liquibase formatted sql

--changeset url-shortener:seed-token-range-001 labels:seed comment:Seed the singleton token-range row
INSERT INTO token_range (id, next_value) VALUES (1, 1000)
ON CONFLICT (id) DO NOTHING;
--rollback DELETE FROM token_range WHERE id = 1;
