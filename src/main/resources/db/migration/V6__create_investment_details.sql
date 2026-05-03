CREATE TABLE investment_details (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id  UUID NOT NULL UNIQUE REFERENCES accounts(id),
    platform    VARCHAR(100),
    instrument  VARCHAR(100),
    asset_type  VARCHAR(50)
);
