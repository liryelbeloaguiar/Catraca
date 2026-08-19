CREATE TABLE queues (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), unit_id UUID NOT NULL REFERENCES units(id),
    name VARCHAR(140) NOT NULL, ticket_prefix VARCHAR(5) NOT NULL, grace_period_minutes INTEGER NOT NULL DEFAULT 0,
    no_show_after_minutes INTEGER NOT NULL DEFAULT 0, automatic_reallocation_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE, UNIQUE(unit_id, name), UNIQUE(unit_id, ticket_prefix)
);
CREATE TABLE ticket_sequences (
    queue_id UUID NOT NULL REFERENCES queues(id) ON DELETE CASCADE, sequence_date DATE NOT NULL,
    last_value BIGINT NOT NULL DEFAULT 0, PRIMARY KEY(queue_id, sequence_date)
);
CREATE TABLE tickets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), queue_id UUID NOT NULL REFERENCES queues(id),
    sequence_date DATE NOT NULL, sequence_number BIGINT NOT NULL, display_code VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), UNIQUE(queue_id, sequence_date, sequence_number)
);
CREATE TABLE queue_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), queue_id UUID NOT NULL REFERENCES queues(id), ticket_id UUID NOT NULL REFERENCES tickets(id),
    appointment_id UUID REFERENCES appointments(id), patient_id UUID NOT NULL REFERENCES patients(id), priority_id UUID REFERENCES priorities(id),
    entered_at TIMESTAMPTZ NOT NULL DEFAULT now(), called_at TIMESTAMPTZ, started_at TIMESTAMPTZ, completed_at TIMESTAMPTZ,
    scheduled_at TIMESTAMPTZ, status VARCHAR(30) NOT NULL DEFAULT 'WAITING', manual_priority BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_queue_ordering ON queue_entries(queue_id, status, entered_at);
CREATE TABLE attendances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), ticket_id UUID NOT NULL REFERENCES tickets(id), patient_id UUID NOT NULL REFERENCES patients(id),
    professional_id UUID REFERENCES professionals(id), attendant_user_id UUID REFERENCES users(id), room_id UUID REFERENCES rooms(id),
    counter_id UUID REFERENCES counters(id), started_at TIMESTAMPTZ, completed_at TIMESTAMPTZ, status VARCHAR(30) NOT NULL
);
