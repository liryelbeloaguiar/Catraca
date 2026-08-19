-- Local development accounts only. This migration is loaded only by the "local" profile.
-- Shared password: 123456. Only its BCrypt hash is persisted.
UPDATE users
SET email = CASE id
        WHEN '10000000-0000-0000-0000-000000000001'::uuid THEN 'admin@teste'
        WHEN '10000000-0000-0000-0000-000000000002'::uuid THEN 'paciente@teste'
        WHEN '10000000-0000-0000-0000-000000000003'::uuid THEN 'medico@teste'
        WHEN '10000000-0000-0000-0000-000000000004'::uuid THEN 'profissional@teste'
        WHEN '10000000-0000-0000-0000-000000000005'::uuid THEN 'recepcao@teste'
        WHEN '10000000-0000-0000-0000-000000000006'::uuid THEN 'guiche@teste'
    END,
    password_hash = '$2a$12$VFEczVWU7rBJOMbqFR0Ma.EgMbUDrp3Mbc1CV/55FaNAUABguyTla',
    failed_login_attempts = 0,
    locked_until = NULL,
    updated_at = now()
WHERE id IN (
    '10000000-0000-0000-0000-000000000001'::uuid,
    '10000000-0000-0000-0000-000000000002'::uuid,
    '10000000-0000-0000-0000-000000000003'::uuid,
    '10000000-0000-0000-0000-000000000004'::uuid,
    '10000000-0000-0000-0000-000000000005'::uuid,
    '10000000-0000-0000-0000-000000000006'::uuid
);
