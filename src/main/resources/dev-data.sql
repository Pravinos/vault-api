-- Seed categories
INSERT INTO categories (name, icon) VALUES
    ('Food',          '🍔'),
    ('Transport',     '🚗'),
    ('Housing',       '🏠'),
    ('Entertainment', '🎮'),
    ('Health',        '💊'),
    ('Shopping',      '🛍️'),
    ('Travel',        '✈️'),
    ('Other',         '📦');

-- Seed income categories
INSERT INTO income_categories (name, icon) VALUES
    ('Salary',    '💼'),
    ('Freelance', '💻'),
    ('Dividend',  '📈'),
    ('Gift',      '🎁'),
    ('Refund',    '↩️'),
    ('Other',     '📦');

-- Seed default account
INSERT INTO accounts (id, name, account_type, opening_balance, created_at)
VALUES ('00000000-0000-0000-0000-000000000001', 'Main Account', 'CHECKING', 0, CURRENT_TIMESTAMP);

-- Seed LLM provider config (required by AI endpoints)
INSERT INTO llm_provider_config (id, chat_provider, chat_model, summary_provider, summary_model, updated_at)
VALUES (1, 'groq', 'llama-3.1-8b-instant', 'groq', 'llama-3.3-70b-versatile', CURRENT_TIMESTAMP);
