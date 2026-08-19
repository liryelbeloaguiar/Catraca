-- Local-only privileged accounts. Shared password: 123456 (BCrypt below).
INSERT INTO users (id, email, password_hash, full_name) VALUES
('10000000-0000-0000-0000-000000000007', 'admin-user@teste', '$2a$12$VFEczVWU7rBJOMbqFR0Ma.EgMbUDrp3Mbc1CV/55FaNAUABguyTla', 'Administrador de Usuários')
ON CONFLICT (email) DO UPDATE SET password_hash = EXCLUDED.password_hash, full_name = EXCLUDED.full_name;

DELETE FROM user_roles
WHERE user_id = '10000000-0000-0000-0000-000000000001';

INSERT INTO user_roles (user_id, role_id)
SELECT '10000000-0000-0000-0000-000000000001', id FROM roles WHERE code = 'DEV_ADMIN';

INSERT INTO user_roles (user_id, role_id)
SELECT '10000000-0000-0000-0000-000000000007', id FROM roles WHERE code = 'ADMIN_USER'
ON CONFLICT DO NOTHING;

INSERT INTO employee_profiles (user_id, employee_number, badge_code, job_title, created_by)
VALUES (
    '10000000-0000-0000-0000-000000000007',
    'ADM-0001',
    'QF-ADM-0001',
    'Administrador de usuários',
    '10000000-0000-0000-0000-000000000001'
)
ON CONFLICT (user_id) DO NOTHING;
