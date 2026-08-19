CREATE TABLE user_profiles (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    phone VARCHAR(30),
    avatar_data BYTEA,
    avatar_content_type VARCHAR(80),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE employee_profiles (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    employee_number VARCHAR(40) NOT NULL UNIQUE,
    badge_code VARCHAR(60) NOT NULL UNIQUE,
    job_title VARCHAR(120) NOT NULL,
    unit_id UUID REFERENCES units(id),
    hired_on DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO roles (code, name) VALUES
('DEV_ADMIN', 'Administrador técnico'),
('ADMIN_USER', 'Administrador de usuários');

INSERT INTO permissions (code, description) VALUES
('PROFILE_MANAGE', 'Gerenciar o próprio perfil'),
('EMPLOYEE_MANAGE', 'Cadastrar e administrar funcionários');

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
CROSS JOIN permissions permission
WHERE role.code = 'DEV_ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
JOIN permissions permission ON permission.code IN (
    'PROFILE_MANAGE', 'EMPLOYEE_MANAGE', 'USER_MANAGE',
    'ADMINISTRATION_MANAGE', 'AUDIT_READ'
)
WHERE role.code = 'ADMIN_USER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
JOIN permissions permission ON permission.code = 'PROFILE_MANAGE'
WHERE role.code NOT IN ('DEV_ADMIN', 'ADMIN_USER')
ON CONFLICT DO NOTHING;
