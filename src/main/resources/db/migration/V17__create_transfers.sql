CREATE TABLE transfers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    from_account_id UUID          NOT NULL REFERENCES accounts(id),
    to_account_id   UUID          NOT NULL REFERENCES accounts(id),
    amount          NUMERIC(10,2) NOT NULL,
    note            VARCHAR(255),
    transfer_date   DATE          NOT NULL DEFAULT CURRENT_DATE,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_different_accounts CHECK (from_account_id != to_account_id)
);

CREATE INDEX idx_transfers_from ON transfers(from_account_id);
CREATE INDEX idx_transfers_to   ON transfers(to_account_id);
