-- disable transaction for this migration
-- flyway:disableTransactions

CREATE TABLE expenses (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    amount       NUMERIC(10, 2) NOT NULL,
    note         VARCHAR(255),
    category_id  INT            NOT NULL REFERENCES categories (id),
    expense_date DATE           NOT NULL DEFAULT CURRENT_DATE,
    created_at   TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_expenses_date     ON expenses (expense_date);
CREATE INDEX idx_expenses_category ON expenses (category_id);