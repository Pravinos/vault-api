-- Create join table linking goals to accounts for account-linked goals
CREATE TABLE goal_accounts (
    goal_id    UUID NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    PRIMARY KEY (goal_id, account_id)
);
