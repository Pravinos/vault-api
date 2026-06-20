CREATE TABLE budgets (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id  INT NOT NULL REFERENCES categories(id),
    month        DATE NOT NULL,
    amount       NUMERIC(10,2) NOT NULL CHECK (amount > 0),
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (category_id, month)
);

CREATE INDEX idx_budgets_month ON budgets(month);
CREATE INDEX idx_budgets_category ON budgets(category_id);
