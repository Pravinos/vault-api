CREATE TABLE income (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    amount             NUMERIC(10,2) NOT NULL,
    note               VARCHAR(255),
    income_category_id INT NOT NULL REFERENCES income_categories(id),
    account_id         UUID NOT NULL REFERENCES accounts(id),
    income_date        DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at         TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_income_date     ON income(income_date);
CREATE INDEX idx_income_category ON income(income_category_id);
CREATE INDEX idx_income_account  ON income(account_id);
