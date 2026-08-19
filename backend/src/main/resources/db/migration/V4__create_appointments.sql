CREATE TABLE appointments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), patient_id UUID NOT NULL REFERENCES patients(id),
    professional_id UUID REFERENCES professionals(id), specialty_id UUID REFERENCES specialties(id),
    service_id UUID NOT NULL REFERENCES services(id), unit_id UUID NOT NULL REFERENCES units(id),
    time_slot_id UUID NOT NULL REFERENCES time_slots(id), status VARCHAR(30) NOT NULL,
    overbook BOOLEAN NOT NULL DEFAULT FALSE, checked_in_at TIMESTAMPTZ, created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_appointments_slot ON appointments(time_slot_id);
CREATE INDEX idx_appointments_patient ON appointments(patient_id, created_at DESC);
CREATE TABLE appointment_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), appointment_id UUID NOT NULL REFERENCES appointments(id) ON DELETE CASCADE,
    previous_status VARCHAR(30), new_status VARCHAR(30) NOT NULL, changed_by UUID NOT NULL REFERENCES users(id),
    reason VARCHAR(255), changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE waiting_list (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), patient_id UUID NOT NULL REFERENCES patients(id),
    service_id UUID NOT NULL REFERENCES services(id), professional_id UUID REFERENCES professionals(id),
    unit_id UUID NOT NULL REFERENCES units(id), desired_from TIMESTAMPTZ NOT NULL, desired_until TIMESTAMPTZ NOT NULL,
    accepts_anticipation BOOLEAN NOT NULL DEFAULT TRUE, status VARCHAR(30) NOT NULL DEFAULT 'WAITING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), CHECK(desired_until > desired_from)
);
