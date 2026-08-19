CREATE TABLE units (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(140) NOT NULL, address VARCHAR(255), active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE departments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), unit_id UUID NOT NULL REFERENCES units(id),
    code VARCHAR(30) NOT NULL, name VARCHAR(140) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE(unit_id, code)
);
CREATE TABLE rooms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), unit_id UUID NOT NULL REFERENCES units(id),
    code VARCHAR(30) NOT NULL, name VARCHAR(140) NOT NULL, floor VARCHAR(30), active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE(unit_id, code)
);
CREATE TABLE counters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), unit_id UUID NOT NULL REFERENCES units(id),
    code VARCHAR(30) NOT NULL, name VARCHAR(140) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE(unit_id, code)
);
CREATE TABLE specialties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(140) NOT NULL, description VARCHAR(255), active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE professional_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(140) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE services (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(140) NOT NULL, description VARCHAR(255), default_duration_minutes INTEGER NOT NULL CHECK(default_duration_minutes > 0),
    requires_professional BOOLEAN NOT NULL DEFAULT TRUE, requires_counter BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE priorities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), name VARCHAR(140) NOT NULL UNIQUE,
    description VARCHAR(255), weight INTEGER NOT NULL CHECK(weight >= 0), display_order INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);
