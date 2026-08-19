CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL
);

CREATE TABLE role_permissions (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(160) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO roles (code, name) VALUES
('ADMIN', 'Administrador'), ('PATIENT', 'Paciente'), ('DOCTOR', 'Médico'),
('PROFESSIONAL', 'Profissional'), ('RECEPTIONIST', 'Recepcionista'),
('COUNTER_ATTENDANT', 'Atendente de guichê');

INSERT INTO permissions (code, description) VALUES
('ADMINISTRATION_MANAGE', 'Administrar configurações operacionais'),
('USER_MANAGE', 'Administrar usuários e acessos'),
('APPOINTMENT_READ', 'Consultar agendamentos'),
('APPOINTMENT_MANAGE', 'Criar e alterar agendamentos'),
('APPOINTMENT_OVERBOOK', 'Autorizar encaixe acima da capacidade'),
('QUEUE_READ', 'Consultar filas'),
('QUEUE_MANAGE', 'Operar filas e fichas'),
('ATTENDANCE_MANAGE', 'Operar atendimentos'),
('AUDIT_READ', 'Consultar auditoria');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.code = 'ADMIN';
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN ('APPOINTMENT_READ', 'APPOINTMENT_MANAGE') WHERE r.code = 'PATIENT';
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN ('APPOINTMENT_READ', 'QUEUE_READ', 'QUEUE_MANAGE', 'ATTENDANCE_MANAGE') WHERE r.code IN ('DOCTOR', 'PROFESSIONAL', 'RECEPTIONIST', 'COUNTER_ATTENDANT');
