CREATE TABLE IF NOT EXISTS display_panels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    unit_id UUID NOT NULL REFERENCES units(id),
    code VARCHAR(40) NOT NULL,
    name VARCHAR(140) NOT NULL,
    floor VARCHAR(30),
    public_token UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    audio_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    voice_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_calls_limit INTEGER NOT NULL DEFAULT 5 CHECK (last_calls_limit BETWEEN 1 AND 20),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (unit_id, code)
);

CREATE TABLE IF NOT EXISTS display_panel_queues (
    panel_id UUID NOT NULL REFERENCES display_panels(id) ON DELETE CASCADE,
    queue_id UUID NOT NULL REFERENCES queues(id) ON DELETE CASCADE,
    PRIMARY KEY (panel_id, queue_id)
);

CREATE TABLE IF NOT EXISTS queue_call_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    panel_id UUID NOT NULL REFERENCES display_panels(id) ON DELETE CASCADE,
    queue_entry_id UUID NOT NULL REFERENCES queue_entries(id),
    ticket_id UUID NOT NULL REFERENCES tickets(id),
    room_id UUID REFERENCES rooms(id),
    counter_id UUID REFERENCES counters(id),
    call_number INTEGER NOT NULL DEFAULT 1 CHECK (call_number > 0),
    called_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    acknowledged_at TIMESTAMPTZ,
    UNIQUE (panel_id, queue_entry_id, call_number)
);

CREATE INDEX IF NOT EXISTS idx_display_panels_unit_floor
    ON display_panels(unit_id, floor, active);
CREATE INDEX IF NOT EXISTS idx_queue_call_events_panel_time
    ON queue_call_events(panel_id, called_at DESC);
