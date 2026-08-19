ALTER TABLE organization_settings
    ADD COLUMN IF NOT EXISTS notification_email VARCHAR(254) NOT NULL
        DEFAULT 'liryelaguiargit@gmail.com';

CREATE TABLE IF NOT EXISTS patient_notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_user_id UUID REFERENCES users(id),
    appointment_id UUID REFERENCES appointments(id),
    notification_type VARCHAR(50) NOT NULL,
    title VARCHAR(180) NOT NULL,
    message TEXT NOT NULL,
    email_to VARCHAR(254),
    email_status VARCHAR(30) NOT NULL DEFAULT 'PENDING_CONFIGURATION',
    email_error VARCHAR(500),
    sent_at TIMESTAMPTZ,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_patient_notifications_recipient
    ON patient_notifications(recipient_user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_patient_notifications_created
    ON patient_notifications(created_at DESC);
