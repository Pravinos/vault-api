INSERT INTO llm_provider_config (
    id,
    active_provider,
    chat_provider,
    chat_model,
    summary_provider,
    summary_model,
    updated_at
)
VALUES (
    1,
    'groq',
    'groq',
    'llama-3.1-8b-instant',
    'groq',
    'llama-3.3-70b-versatile',
    NOW()
)
ON CONFLICT (id) DO UPDATE
SET active_provider = EXCLUDED.active_provider,
    chat_provider = EXCLUDED.chat_provider,
    chat_model = EXCLUDED.chat_model,
    summary_provider = EXCLUDED.summary_provider,
    summary_model = EXCLUDED.summary_model,
    updated_at = NOW();

ALTER TABLE llm_provider_config
    ALTER COLUMN chat_provider SET DEFAULT 'groq',
    ALTER COLUMN chat_model SET DEFAULT 'llama-3.1-8b-instant',
    ALTER COLUMN summary_provider SET DEFAULT 'groq',
    ALTER COLUMN summary_model SET DEFAULT 'llama-3.3-70b-versatile';
