CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), actor_user_id UUID REFERENCES users(id), action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(80) NOT NULL, resource_id UUID, details JSONB NOT NULL DEFAULT '{}'::jsonb,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(), ip_address INET
);
CREATE INDEX idx_audit_occurred_at ON audit_logs(occurred_at DESC);
