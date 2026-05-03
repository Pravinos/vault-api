CREATE TABLE investment_checkpoints (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id  UUID NOT NULL REFERENCES accounts(id),
    value       NUMERIC(10,2) NOT NULL,
    recorded_at TIMESTAMP NOT NULL DEFAULT NOW(),
    note        VARCHAR(255)
);

CREATE INDEX idx_checkpoints_account ON investment_checkpoints(account_id);
CREATE INDEX idx_checkpoints_date    ON investment_checkpoints(recorded_at);
