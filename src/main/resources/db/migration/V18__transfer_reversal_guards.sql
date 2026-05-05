ALTER TABLE transfers
    ADD COLUMN original_transfer_id UUID REFERENCES transfers(id),
    ADD COLUMN is_reversal BOOLEAN NOT NULL DEFAULT FALSE;

CREATE UNIQUE INDEX ux_transfers_original_transfer_id
    ON transfers(original_transfer_id)
    WHERE original_transfer_id IS NOT NULL;
