-- Local development accounts only. This migration is loaded only by the "local" profile.
-- The shared password is documented in TEST-USERS.md; only its BCrypt hash is stored here.
INSERT INTO users (id, email, password_hash, full_name) VALUES
('10000000-0000-0000-0000-000000000001', 'admin.test@queueflow.local', '$2a$12$S2OKk2Qv8gLSMgLaMSxIf.b7j3HB6UhDzl27c9Bvb0gkOWZwIabwG', 'Administrador de Teste'),
('10000000-0000-0000-0000-000000000002', 'patient.test@queueflow.local', '$2a$12$S2OKk2Qv8gLSMgLaMSxIf.b7j3HB6UhDzl27c9Bvb0gkOWZwIabwG', 'Paciente de Teste'),
('10000000-0000-0000-0000-000000000003', 'doctor.test@queueflow.local', '$2a$12$S2OKk2Qv8gLSMgLaMSxIf.b7j3HB6UhDzl27c9Bvb0gkOWZwIabwG', 'Médico de Teste'),
('10000000-0000-0000-0000-000000000004', 'professional.test@queueflow.local', '$2a$12$S2OKk2Qv8gLSMgLaMSxIf.b7j3HB6UhDzl27c9Bvb0gkOWZwIabwG', 'Profissional de Teste'),
('10000000-0000-0000-0000-000000000005', 'receptionist.test@queueflow.local', '$2a$12$S2OKk2Qv8gLSMgLaMSxIf.b7j3HB6UhDzl27c9Bvb0gkOWZwIabwG', 'Recepcionista de Teste'),
('10000000-0000-0000-0000-000000000006', 'counter.test@queueflow.local', '$2a$12$S2OKk2Qv8gLSMgLaMSxIf.b7j3HB6UhDzl27c9Bvb0gkOWZwIabwG', 'Atendente de Guichê de Teste');

INSERT INTO user_roles (user_id, role_id)
SELECT test_user.user_id, role.id
FROM (VALUES
    ('10000000-0000-0000-0000-000000000001'::uuid, 'ADMIN'),
    ('10000000-0000-0000-0000-000000000002'::uuid, 'PATIENT'),
    ('10000000-0000-0000-0000-000000000003'::uuid, 'DOCTOR'),
    ('10000000-0000-0000-0000-000000000004'::uuid, 'PROFESSIONAL'),
    ('10000000-0000-0000-0000-000000000005'::uuid, 'RECEPTIONIST'),
    ('10000000-0000-0000-0000-000000000006'::uuid, 'COUNTER_ATTENDANT')
) AS test_user(user_id, role_code)
JOIN roles role ON role.code = test_user.role_code;
