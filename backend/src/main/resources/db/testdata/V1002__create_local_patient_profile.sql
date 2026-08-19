-- Profile required for the local PATIENT test account to exercise self-service flows.
INSERT INTO patients (id, user_id, document, birth_date, phone, registration_status)
VALUES (
    '20000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000002',
    'LOCAL-PATIENT-TEST',
    DATE '1990-01-01',
    NULL,
    'ACTIVE'
)
ON CONFLICT (user_id) DO NOTHING;
