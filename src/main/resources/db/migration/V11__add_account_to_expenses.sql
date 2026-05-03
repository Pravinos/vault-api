-- Add nullable first
ALTER TABLE expenses ADD COLUMN account_id UUID REFERENCES accounts(id);

-- Assign all existing expenses to the default account
UPDATE expenses SET account_id = '00000000-0000-0000-0000-000000000001'
WHERE account_id IS NULL;

-- Now enforce NOT NULL
ALTER TABLE expenses ALTER COLUMN account_id SET NOT NULL;
