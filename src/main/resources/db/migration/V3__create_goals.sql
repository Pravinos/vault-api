-- disable transaction for this migration
-- flyway:disableTransactions

CREATE TABLE goals (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name           VARCHAR(100)   NOT NULL,
    description    VARCHAR(255),
    target_amount  NUMERIC(10, 2) NOT NULL,
    saved_amount   NUMERIC(10, 2) NOT NULL DEFAULT 0,
    goal_type      VARCHAR(20)    NOT NULL,
    deadline       DATE,
    created_at     TIMESTAMP      NOT NULL DEFAULT NOW(),
    is_active      BOOLEAN        NOT NULL DEFAULT TRUE
);