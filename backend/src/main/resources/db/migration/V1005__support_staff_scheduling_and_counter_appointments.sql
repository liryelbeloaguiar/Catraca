ALTER TABLE appointments ALTER COLUMN patient_id DROP NOT NULL;
ALTER TABLE appointments ADD COLUMN guest_name VARCHAR(160);
ALTER TABLE appointments ADD COLUMN counter_id UUID REFERENCES counters(id);
ALTER TABLE appointments ADD CONSTRAINT ck_appointment_patient_or_guest
    CHECK (patient_id IS NOT NULL OR length(trim(guest_name)) >= 2);
CREATE INDEX idx_appointments_counter ON appointments(counter_id, created_at DESC);

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
JOIN permissions permission ON permission.code IN ('APPOINTMENT_READ', 'APPOINTMENT_MANAGE', 'APPOINTMENT_OVERBOOK')
WHERE role.code IN ('ADMIN_USER', 'RECEPTIONIST', 'COUNTER_ATTENDANT')
ON CONFLICT DO NOTHING;
