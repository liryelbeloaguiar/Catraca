CREATE TABLE patients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), user_id UUID UNIQUE REFERENCES users(id),
    document VARCHAR(40) NOT NULL UNIQUE, birth_date DATE NOT NULL, phone VARCHAR(30),
    registration_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE', created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE professionals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), user_id UUID UNIQUE REFERENCES users(id),
    professional_type_id UUID NOT NULL REFERENCES professional_types(id), registration_number VARCHAR(60),
    default_duration_minutes INTEGER NOT NULL CHECK(default_duration_minutes > 0), active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE professional_specialties (
    professional_id UUID NOT NULL REFERENCES professionals(id) ON DELETE CASCADE,
    specialty_id UUID NOT NULL REFERENCES specialties(id), PRIMARY KEY(professional_id, specialty_id)
);
CREATE TABLE schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), professional_id UUID NOT NULL REFERENCES professionals(id),
    unit_id UUID NOT NULL REFERENCES units(id), room_id UUID REFERENCES rooms(id),
    valid_from DATE NOT NULL, valid_until DATE, active BOOLEAN NOT NULL DEFAULT TRUE,
    CHECK(valid_until IS NULL OR valid_until >= valid_from)
);
CREATE TABLE shifts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), schedule_id UUID NOT NULL REFERENCES schedules(id) ON DELETE CASCADE,
    day_of_week SMALLINT NOT NULL CHECK(day_of_week BETWEEN 1 AND 7), start_time TIME NOT NULL, end_time TIME NOT NULL,
    break_start TIME, break_end TIME, slot_duration_minutes INTEGER NOT NULL CHECK(slot_duration_minutes > 0),
    default_capacity INTEGER NOT NULL CHECK(default_capacity > 0), CHECK(end_time > start_time)
);
CREATE TABLE time_slots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), schedule_id UUID NOT NULL REFERENCES schedules(id),
    starts_at TIMESTAMPTZ NOT NULL, ends_at TIMESTAMPTZ NOT NULL, capacity INTEGER NOT NULL CHECK(capacity > 0),
    booked_count INTEGER NOT NULL DEFAULT 0 CHECK(booked_count >= 0 AND booked_count <= capacity),
    blocked BOOLEAN NOT NULL DEFAULT FALSE, version BIGINT NOT NULL DEFAULT 0, UNIQUE(schedule_id, starts_at), CHECK(ends_at > starts_at)
);
CREATE TABLE schedule_exceptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), schedule_id UUID NOT NULL REFERENCES schedules(id),
    starts_at TIMESTAMPTZ NOT NULL, ends_at TIMESTAMPTZ NOT NULL, reason VARCHAR(255) NOT NULL,
    exception_type VARCHAR(30) NOT NULL, CHECK(ends_at > starts_at)
);
