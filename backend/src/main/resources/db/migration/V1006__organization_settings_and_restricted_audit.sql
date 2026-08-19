CREATE SEQUENCE employee_number_sequence START WITH 1 INCREMENT BY 1;

CREATE TABLE organization_settings (
    id SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    establishment_name VARCHAR(160) NOT NULL,
    updated_by UUID REFERENCES users(id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO organization_settings (id, establishment_name)
VALUES (1, 'QueueFlow');

DELETE FROM role_permissions role_permission
USING roles role, permissions permission
WHERE role_permission.role_id = role.id
  AND role_permission.permission_id = permission.id
  AND permission.code = 'AUDIT_READ'
  AND role.code <> 'DEV_ADMIN';
