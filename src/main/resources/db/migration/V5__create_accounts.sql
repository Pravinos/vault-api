CREATE TYPE account_type AS ENUM ('CHECKING', 'SAVINGS', 'INVESTMENT');

CREATE TABLE accounts (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                      VARCHAR(100) NOT NULL,
    account_type              account_type NOT NULL,
    opening_balance           NUMERIC(10,2) NOT NULL DEFAULT 0,
    manual_balance            NUMERIC(10,2),
    manual_balance_updated_at TIMESTAMP,
    created_at                TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active                 BOOLEAN NOT NULL DEFAULT TRUE
);
