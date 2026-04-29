--liquibase formatted sql

--changeset auth-service:seed-users-001 labels:seed comment:Seed initial users (passwords: manager123, user123, admin123)
INSERT INTO auth_user (id, email, password_hash, name, role) VALUES
    (1, 'manager1@hotels.com', '516c182a9d649a48ca75883d070c92eef1e41a2e2882b5d8d53abba0bebf0067', 'Sofia Rossi',  'MANAGER'),
    (2, 'manager2@hotels.com', '516c182a9d649a48ca75883d070c92eef1e41a2e2882b5d8d53abba0bebf0067', 'Carlos Mendez', 'MANAGER'),
    (3, 'manager3@hotels.com', '516c182a9d649a48ca75883d070c92eef1e41a2e2882b5d8d53abba0bebf0067', 'Yuki Tanaka',   'MANAGER'),
    (4, 'tasnim@guests.com', '9a325b7c1140d49dd72f147ca2b166dbb4e0fc3b772755d2997449fb718700f6', 'Tasnim', 'USER'),
    (5, 'amira@guests.com',  '9a325b7c1140d49dd72f147ca2b166dbb4e0fc3b772755d2997449fb718700f6', 'Amira',  'USER'),
    (6, 'mariam@guests.com', '9a325b7c1140d49dd72f147ca2b166dbb4e0fc3b772755d2997449fb718700f6', 'Mariam', 'USER'),
    (7, 'hajar@guests.com',  '9a325b7c1140d49dd72f147ca2b166dbb4e0fc3b772755d2997449fb718700f6', 'Hajar',  'USER'),
    (8, 'admin@booking.com',  '0be10ead37a280e14a0917bf706bfa9873096eaeabb55a5129ca96c52cca5eaa', 'System Admin',  'ADMIN')
ON CONFLICT (id) DO NOTHING;
--rollback DELETE FROM auth_user WHERE id BETWEEN 1 AND 8;

--changeset auth-service:seed-users-002 labels:seed comment:Reset auth_user id sequence past seeded rows
SELECT setval('auth_user_id_seq', GREATEST((SELECT MAX(id) FROM auth_user), 8));
--rollback SELECT 1;
